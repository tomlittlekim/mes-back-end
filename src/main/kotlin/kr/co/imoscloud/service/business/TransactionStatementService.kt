package kr.co.imoscloud.service.business

import jakarta.servlet.http.HttpServletResponse
import jakarta.transaction.Transactional
import kr.co.imoscloud.constants.CoreEnum
import kr.co.imoscloud.core.CompanyCacheManager
import kr.co.imoscloud.core.UserCacheManager
import kr.co.imoscloud.entity.business.TransactionStatementHeader
import kr.co.imoscloud.entity.drive.FileManagement
import kr.co.imoscloud.repository.business.TransactionStatementDetailRepository
import kr.co.imoscloud.repository.business.TransactionStatementHeaderRepository
import kr.co.imoscloud.repository.drive.DriveRepository
import kr.co.imoscloud.service.drive.FileConvertService
import kr.co.imoscloud.util.AbstractPrint
import kr.co.imoscloud.util.DateUtils
import kr.co.imoscloud.util.PrintDto
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class TransactionStatementService(
    val ucm: UserCacheManager,
    val ccm: CompanyCacheManager,
    val headerRepo: TransactionStatementHeaderRepository,
    val detailRepo: TransactionStatementDetailRepository,
    private val driveRepo: DriveRepository,
    private val convertService: FileConvertService,
): AbstractPrint(ucm, ccm, convertService) {

    override fun getFodsFile(): FileManagement {
        val menuId = "TS"
        return driveRepo.findByMenuIdAndFlagActiveIsTrue(menuId)
            ?: throw IllegalArgumentException("해당 메뉴는 출력 기능을 지원하지 않습니다. ")
    }

    override fun <B> entityToPrintDto(body: B): PrintDto {
        return if (body is TransactionStatementDetailNullableDto) {
            PrintDto(
                body.materialName,
                body.materialStandard,
                body.shippedQuantity?.toInt().toString(),
                formattedNumber(body.unitPrice),
                formattedNumber(body.supplyPrice),
                formattedNumber(body.vat)
            )
        } else {
            throw IllegalArgumentException("지원하지 않는 객체입니다. ")
        }
    }

    override fun <H> extractAdditionalHeaderFields(header: H?): MutableMap<String, String>? {
        val result = mutableMapOf<String, String>()

        if (header != null && header is TransactionStatementPrintRequest) {
            result["no"] = "1"
            result["localDate"] = DateUtils.formattedDate(header.transactionDate, CoreEnum.DateTimeFormat.YEAR_MONTH_DAY_KOR)
            result["customer"] = header.customerName
            result["title"] = "거레명세서"
        }

        return if (result.isEmpty()) null else result
    }

    override fun <B> extractAdditionalBodyFields(bodies: List<B>): MutableMap<String, String>? {
        val result = mutableMapOf<String, String>()
        val details = bodies.filterIsInstance<TransactionStatementDetailNullableDto>()
        if (details.isEmpty()) return null

        var totalQty: Double = 0.0
        var totalPrice: Int = 0
        var totalAmount: Int = 0
        var totalVat: Int = 0
        var finalPrice: Int = 0

        for (detail in details) {
            totalQty += detail.shippedQuantity ?: 0.0
            totalPrice += detail.unitPrice ?: 0
            totalAmount += detail.supplyPrice ?: 0
            totalVat += detail.vat ?: 0
            finalPrice += ((detail.supplyPrice ?: 0)+(detail.vat ?: 0))
        }

        result["totalQty"] = totalQty.toInt().toString()
        result["totalPrice"] = formattedNumber(totalPrice)
        result["totalSupplyPrice"] = formattedNumber(totalAmount)
        result["totalVat"] = formattedNumber(totalVat)
        result["finalPrice"] = formattedNumber(finalPrice)
        return result
    }

    fun getAllBySearchCondition(req: TransactionStatementSearchCondition): List<TransactionStatementHeaderNullableDto> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val (from, to) = DateUtils.getSearchDateRange(req.fromDate, req.toDate)

        return headerRepo.getAllBySearchCondition(
            loginUser.getSite(),
            loginUser.compCd,
            req.id,
            from, to,
            req.orderNo,
            req.customerId
        )
    }

    fun getAllDetailsByOrderNo(orderNo: String): List<TransactionStatementDetailNullableDto> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        return detailRepo.getAllInitialByOrderNo(loginUser.getSite(), loginUser.compCd, orderNo)
            .map { it.apply {
                val qty = it.shippedQuantity?.toInt() ?: 0
                val price = qty * it.unitPrice!!
                supplyPrice = price
                vat = price / 10
            } }
    }

    @Transactional
    fun printProcess(req: TransactionStatementPrintRequest, hsr: HttpServletResponse): Unit {
        val header = headerRepo.findByIdAndFlagActiveIsTrue(req.headerId)
            ?: throw IllegalArgumentException("선택한 거래 명세서 정보가 존재하지 않습니다. ")

        val finalDetails = getAllDetailsByOrderNo(header.orderNo)
        val bodies = finalDetails.filter { req.detailIds.contains(it.id) }

        process(req, bodies, hsr)

        updateHeaderAndDetails(header, req.detailIds, req)
    }

    @Transactional
    fun softDeleteByOrderNo(orderNo: String, isRemoveParent: Boolean?=false): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val header = headerRepo
            .findBySiteAndCompCdAndOrderNoAndFlagActiveIsTrue(loginUser.getSite(), loginUser.compCd, orderNo)
            ?.let { it.apply {
                flagIssuance = false
                issuanceDate = null
                if (isRemoveParent == true) flagActive = false
                updateCommonCol(loginUser)
            } }
            ?: throw IllegalArgumentException("선택한 거래 명세서 정보가 존재하지 않습니다. ")

        val details = detailRepo.findAllBySiteAndCompCdAndOrderNoAndFlagActiveIsTrue(
            header.site,
            header.compCd,
            header.orderNo
        ).map { detail ->
            detail.apply {
                transactionStatementId = null
                transactionStatementDate = null
                if (isRemoveParent == true) flagActive = false
                updateCommonCol(loginUser)
            }
        }

        headerRepo.save(header)
        detailRepo.saveAll(details)
        return "삭제 성공"
    }

    private fun updateHeaderAndDetails(
        header: TransactionStatementHeader,
        detailIds: List<Long>,
        req: TransactionStatementPrintRequest
    ): Unit {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val localDate = DateUtils.parseDate(req.transactionDate)
        val today = LocalDateTime.now()
        val formatDate = DateUtils.formattedDate(today, CoreEnum.DateTimeFormat.MOS_EVENT_TIME)
        val tsId = formatDate.substring(2, formatDate.length - 1)

        headerRepo.save(header.apply {
            flagIssuance = true
            issuanceDate = today.toLocalDate()
            updateCommonCol(loginUser)
        })

        val result = detailRepo.updateTSByIdIn(
            tsId,
            (localDate?:today.toLocalDate()),
            loginUser.loginId,
            loginUser.compCd,
            detailIds
        )

        if (result==0) throw IllegalArgumentException("거래명세서 상세 저장에 오류가 발생했습니다. ")
    }
}

data class TransactionStatementPrintRequest(
    val headerId: Long,
    val transactionDate: String,
    val customerName: String,
    val detailIds: List<Long>
)

data class TransactionStatementSearchCondition(
    val id: Long?=null,
    val fromDate: String? = null,
    val toDate: String? = null,
    val orderNo: String? = null,
    val customerId: String? = null,
)

data class TransactionStatementHeaderNullableDto(
    val id: Long? = null,
    val site: String? = null,
    val compCd: String? = null,
    val orderNo: String? = null,
    val orderDate: LocalDate? = null,
    val customerName: String? = null,
    val orderQuantity: Double? = null,
    val totalAmount: Int? = 0,

    var supplyPrice: Int? = 0,
    var vat: Int? = 0,
    val flagIssuance: Boolean? = false,
    val issuanceDate: LocalDate? = null,

    val flagVat: Boolean? = false,
)

data class TransactionStatementDetailNullableDto(
    val id: Long? = null,
    val site: String? = null,
    val compCd: String? = null,
    val orderNo: String? = null,
    val orderSubNo: String? = null,
    val transactionStatementId: String? = null,
    val transactionStatementDate: LocalDate? = null,
    //ShipmentDetail
    var systemMaterialId: String? = null,
    var materialName: String? = null,
    var materialStandard: String? = null,
    var unit: String? = null,
    var shippedQuantity: Double? = 0.0,
    //OrderDetail
    val unitPrice: Int? = 0,
    //공급가 (quantity * unitPrice).toInt()
    var supplyPrice: Int? = 0,
    //선택한 Header 의 flagVat 분기로 /10
    var vat: Int? = 0,
)

data class ShipmentDetailWithMaterialDto(
    val orderNo: String? = null,
    val orderSubNo: String? = null,
    val systemMaterialId: String? = null,
    val materialName: String? = null,
    val materialStandard: String? = null,
    val unit: String? = null,
    var quantity: Double? = null,
)

data class ShipmentWithSupplyPrice(
    val orderNo: String,
    val supplyPrice: Int,
)