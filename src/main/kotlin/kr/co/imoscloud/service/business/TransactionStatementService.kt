package kr.co.imoscloud.service.business

import jakarta.servlet.http.HttpServletResponse
import jakarta.transaction.Transactional
import kr.co.imoscloud.constants.CoreEnum
import kr.co.imoscloud.core.Core
import kr.co.imoscloud.entity.business.OrderDetail
import kr.co.imoscloud.entity.business.TransactionStatementDetail
import kr.co.imoscloud.entity.business.TransactionStatementHeader
import kr.co.imoscloud.entity.drive.FileManagement
import kr.co.imoscloud.iface.IDrive
import kr.co.imoscloud.repository.business.ShipmentDetailRepository
import kr.co.imoscloud.repository.business.TransactionStatementDetailRepository
import kr.co.imoscloud.repository.business.TransactionStatementHeaderRepository
import kr.co.imoscloud.repository.drive.DriveRepository
import kr.co.imoscloud.service.drive.FileConvertService
import kr.co.imoscloud.util.AbstractPrint
import kr.co.imoscloud.util.DateUtils
import kr.co.imoscloud.util.PrintDto
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class TransactionStatementService(
    val core: Core,
    private val headerRepo: TransactionStatementHeaderRepository,
    private val detailRepo: TransactionStatementDetailRepository,
    private val driveRepo: DriveRepository,
    private val orderService: OrderService,
    private val convertService: FileConvertService,
    private val shipmentDetailRepo: ShipmentDetailRepository
): AbstractPrint(core, convertService), IDrive {

    private val MENU_ID = ""

    override fun getFodsFile(): FileManagement {
        return driveRepo.findByMenuIdAndFlagActiveIsTrue(MENU_ID)
            ?: throw IllegalArgumentException("해당 메뉴는 출력 기능을 지원하지 않습니다. ")
    }

    override fun <B> entityToPrintDto(body: B): PrintDto {
        return if (body is OrderDetailNullableDto) {
            PrintDto(
                body.materialName,
                body.materialStandard,
                body.quantity.toString(),
                body.unitPrice.toString(),
                body.supplyPrice.toString(),
                body.vatPrice.toString()
            )
        } else {
            throw IllegalArgumentException("지원하지 않는 객체입니다. ")
        }
    }

    override fun <H> extractAdditionalHeaderFields(header: H?): MutableMap<String, String>? {
        val result = mutableMapOf<String, String>()

        if (header != null && header is TransactionStatementPrintRequest) {
            result["no"] = "1"
            result["localDate"] = formattedDate(header.transactionDate, CoreEnum.DateTimeFormat.YEAR_MONTH_DAY_KOR)
            result["customer"] = header.customerName
        }

        return if (result.isEmpty()) null else result
    }

    override fun <B> extractAdditionalBodyFields(bodies: List<B>): MutableMap<String, String>? {
        val result = mutableMapOf<String, String>()
        val details = bodies.filterIsInstance<OrderDetail>()
        if (details.isEmpty()) return null

        var totalQty: Double = 0.0
        var totalPrice: Int = 0
        var totalAmount: Int = 0
        var totalVat: Int = 0
        var finalPrice: Int = 0

        for (detail in details) {
            totalQty += detail.quantity ?: 0.0
            totalPrice += detail.unitPrice ?: 0
            totalAmount += detail.supplyPrice ?: 0
            totalVat += detail.vatPrice ?: 0
            finalPrice += ((detail.supplyPrice ?: 0)+(detail.vatPrice ?: 0))
        }

        result["totalQty"] = totalQty.toString()
        result["totalPrice"] = totalPrice.toString()
        result["totalAmount"] = totalAmount.toString()
        result["totalVat"] = totalVat.toString()
        result["finalPrice"] = finalPrice.toString()
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

        val latestMap = detailRepo.getAllLatestByOrderNo(loginUser.getSite(), loginUser.compCd, orderNo)
            .associateBy { it.orderNo }

        return detailRepo.getAllInitialByOrderNo(loginUser.getSite(), loginUser.compCd, orderNo)
            .mapNotNull { detail ->
                latestMap[detail.orderNo]?.let { latest ->
                    detail.apply {
                        systemMaterialId = latest.systemMaterialId
                        materialName = latest.materialName
                        materialStandard = latest.materialStandard
                        unit = latest.unit
                        shippedQuantity = latest.quantity
                    }
                }
            }
    }

    @Transactional
    fun printProcess(req: TransactionStatementPrintRequest, response: HttpServletResponse): Unit {
        val header = headerRepo.findByIdAndFlagActiveIsTrue(req.headerId)
            ?: throw IllegalArgumentException("선택한 거래 명세서 정보가 존재하지 않습니다. ")

        var finalDetails = getAllDetailsByOrderNo(header.orderNo)

        val details = detailRepo.findAllByIdInAndFlagActiveIsTrue(req.detailIds)
        val selectedDetailIds = details.map { it.id }
        finalDetails = finalDetails.filter { selectedDetailIds.contains(it.id) }

        val pdfFile: File = process(req, finalDetails)
        val encodedFileName = encodeToString("${req.transactionDate}_${req.customerName}_거래명세서.pdf")
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''${encodedFileName}")
        response.contentType = "application/octet-stream"
        response.setContentLength(pdfFile.length().toInt())
        FileInputStream(pdfFile).use { it.copyTo(response.outputStream) }

        pdfFile.delete()
        updateHeaderAndDetails(header, details, req)
    }

    @Transactional
    fun softDeleteByOrderNo(headerId: Long): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val header = headerRepo.findByIdAndFlagActiveIsTrue(headerId)
            ?.let { it.apply {
                flagIssuance = false
                issuanceDate = null
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
                updateCommonCol(loginUser)
            }
        }

        headerRepo.save(header)
        detailRepo.saveAll(details)
        return "삭제 성공"
    }

    private fun updateHeaderAndDetails(
        header: TransactionStatementHeader,
        details: List<TransactionStatementDetail>,
        req: TransactionStatementPrintRequest
    ): Unit {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val localDate = DateUtils.parseDate(req.transactionDate)
        val today = LocalDateTime.now()
        val formatDate = formattedDate(today, CoreEnum.DateTimeFormat.MOS_EVENT_TIME)
        val tsId = formatDate.substring(2, formatDate.length - 1)

        headerRepo.save(header.apply {
            flagIssuance = true
            issuanceDate = today.toLocalDate()
            updateCommonCol(loginUser)
        })
        if (details.isNotEmpty()){
            val updates = details.map { detail ->
                detail.apply {
                    transactionStatementId = tsId
                    transactionStatementDate = localDate
                    updateCommonCol(loginUser)
                }
            }
            detailRepo.saveAll(updates)
        }
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
    //공급가 (shippedQuantity * unitPrice).toInt()
    val supplyPrice: Int? = 0,
    //선택한 Header 의 flagVat 분기로 /10
    val vat: Int? = 0,
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