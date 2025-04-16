package kr.co.imoscloud.service.business

import kr.co.imoscloud.entity.business.OrderDetail
import kr.co.imoscloud.entity.business.OrderHeader
import kr.co.imoscloud.entity.material.MaterialMaster
import kr.co.imoscloud.repository.VendorRep
import kr.co.imoscloud.repository.business.OrderDetailRepository
import kr.co.imoscloud.repository.business.OrderHeaderRepository
import kr.co.imoscloud.repository.material.MaterialRepository
import kr.co.imoscloud.util.DateUtils
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class OrderService(
    val headerRepo: OrderHeaderRepository,
    private val vendorRepo: VendorRep,
    val detailRepo: OrderDetailRepository,
    private val materialRepo: MaterialRepository
) {

    // orderHeader 조회
    fun getHeadersBySearchRequestByCompCd(req: OrderHeaderSearchRequest): List<OrderHeaderNullableDto> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val (from, to) = DateUtils.getSearchDateRange(req.fromDate, req.toDate)

        val headerList = req.materialId
            ?.let { detailRepo.findAllBySearchCondition(loginUser.compCd, req.orderNo, from, to, req.customerId, req.materialId) }
            ?:run { headerRepo.findAllBySearchCondition(loginUser.compCd, req.orderNo, from, to, req.customerId) }

        return headerList.map { headerToResponse(it) }
    }

    fun addHeader(): OrderHeaderNullableDto {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val latestNo = headerRepo.getLatestOrderNo(loginUser.compCd)

        val nextVersion = latestNo
            ?.takeLast(3)
            ?.trim()
            ?.toIntOrNull()
            ?.plus(1)
            ?.let { "%03d".format(it) }
            ?: "001"

        val prefix = "${loginUser.compCd}${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))}"

        return OrderHeaderNullableDto(
            site = loginUser.getSite(),
            compCd = loginUser.compCd,
            orderNo = "$prefix$nextVersion",
        )
    }

    fun deleteHeader(id: Long): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val header = headerRepo.findBySiteAndCompCdAndIdAndFlagActiveIsTrue(loginUser.getSite(), loginUser.compCd, id)
            ?.apply { flagActive = false; updateCommonCol(loginUser) }
            ?: throw IllegalArgumentException("기본 주문정보가 존재하지 않습니다. ")

        headerRepo.save(header)
        return "${header.orderNo} 삭제 성공"
    }

    fun upsertHeader(list: List<OrderHeaderRequest>): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val indies = list.mapNotNull { it.id }
        val headerMap = headerRepo
            .findAllBySiteAndCompCdAndIdInAndFlagActiveIsTrue(loginUser.getSite(), loginUser.compCd, indies)
            .associateBy { it.id }

        val headerList: List<OrderHeader> = list.map { req ->
            headerMap[req.id]
                ?.apply {
                    orderDate = DateUtils.parseDate(req.orderDate)
                    customerId = req.customerId
                    ordererId = req.orderer
                    flagVatAmount = req.flagVatAmount ?: this.flagVatAmount
                    deliveryDate = DateUtils.parseDateTime(req.deliveryDate)
                    paymentMethod = req.paymentMethod
                    deliveryAddr = req.deliveryAddr
                    remark = req.remark
                    updateCommonCol(loginUser)
                }
                ?:run {
                    try {
                        OrderHeader(
                            site = req.site!!,
                            compCd = req.compCd!!,
                            orderNo = req.orderNo!!,
                            orderDate = DateUtils.parseDate(req.orderDate),
                            customerId = req.customerId,
                            flagVatAmount = req.flagVatAmount!!,
                            deliveryDate = DateUtils.parseDateTime(req.deliveryDate),
                            paymentMethod = req.paymentMethod,
                            deliveryAddr = req.deliveryAddr,
                            remark = req.remark,
                        )
                    } catch (e: NullPointerException) {
                        throw IllegalArgumentException("기본 주문정보를 생성할 필드값이 부족합니다. ")
                    }
                }
            }

        headerRepo.saveAll(headerList)
        return "기본 주문정보 생성 및 수정 성공"
    }



    fun getDetailsByOrderNo(orderNo: String): List<OrderDetailNullableDto> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val detailList = detailRepo.findAllByOrderNoAndCompCdAndFlagActiveIsTrue(loginUser.compCd, orderNo)

        val indies = detailList.map { it.systemMaterialId }
        val materialMap = materialRepo.getMaterialListByIds(loginUser.getSite(), loginUser.compCd, indies)
            .associateBy { it?.systemMaterialId }

        return detailList.map { detailToResponse(it, materialMap) }
    }

    fun addDetail(header: OrderHeaderNullableDto): OrderDetailNullableDto {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        if (!header.compCd.equals(loginUser.compCd)) throw IllegalArgumentException("다른 회사의 정보를 조회할 수 없습니다. ")

        val latestSubOrderNo = detailRepo.getLatestOrderSubNo(header.compCd!!)
        val nextVersion = latestSubOrderNo
            ?.takeLast(3)
            ?.trim()
            ?.toIntOrNull()
            ?.plus(1)
            ?.let { "%03d".format(it) }
            ?: "001"

        return OrderDetailNullableDto(
            site = header.site,
            compCd = header.compCd,
            orderNo = header.orderNo,
            orderSubNo = nextVersion,
        )
    }

    fun deleteDetail(id: Long): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val detail = detailRepo.findBySiteAndCompCdAndIdAndFlagActiveIsTrue(loginUser.getSite(), loginUser.compCd, id)
            ?.apply { flagActive = false; updateCommonCol(loginUser) }
            ?: throw IllegalArgumentException("주문상세정보가 존재하지 않습니다. ")

        detailRepo.save(detail)
        return "${detail.orderNo} - ${detail.orderSubNo} 삭제 성공"
    }


    private fun headerToResponse(header: OrderHeader): OrderHeaderNullableDto = OrderHeaderNullableDto(
        id = header.id,
        site = header.site,
        compCd = header.compCd,
        orderNo = header.orderNo,
        customerId = header.customerId,
        totalAmount = header.totalAmount,
        vatAmount = header.vatAmount,
        flagVatAmount = header.flagVatAmount,
        finalAmount = header.finalAmount,
        deliveryDate = header.deliveryDate,
        paymentMethod = header.paymentMethod,
        deliveryAddr = header.deliveryAddr,
        remark = header.remark,

        createUser = header.createUser,
        createDate = header.createDate,
        updateUser = header.updateUser,
        updateDate = header.updateDate,
        flagActive = header.flagActive,
    )

    private fun detailToResponse(
        detail: OrderDetail,
        map: Map<String?, MaterialMaster?>
    ): OrderDetailNullableDto {
        val materialInfo = map[detail.systemMaterialId]

        return OrderDetailNullableDto(
            id = detail.id,
            site = detail.site,
            compCd = detail.compCd,
            orderNo = detail.orderNo,
            orderSubNo = detail.orderSubNo,
            systemMaterialId = materialInfo?.systemMaterialId,
            materialName = materialInfo?.materialName,
            materialStandard = materialInfo?.materialStandard,
            unit = materialInfo?.unit,
            deliveryDate = detail.deliveryDate,
            quantity = detail.quantity,
            unitPrice = detail.unitPrice,
            supplyPrice = detail.supplyPrice,
            vatPrice = detail.vatPrice,
            totalPrice = detail.totalPrice,
            remark = detail.remark,

            createUser = detail.createUser,
            createDate = detail.createDate,
            updateUser = detail.updateUser,
            updateDate = detail.updateDate,
            flagActive = detail.flagActive,
        )
    }

    data class OrderHeaderSearchRequest(
        val orderNo: String?=null,
        val fromDate: String?=null,
        val toDate: String?=null,
        val customerId: String?=null,
        val materialId: String?=null,
    )

    data class OrderHeaderNullableDto(
        val id: Long? = null,
        val site: String? = null,
        val compCd: String? = null,
        val orderNo: String? = null,
        val orderDate: LocalDate? = null,
        val customerId: String? = null,
        val totalAmount: Int? = 0,
        val vatAmount: Int? = 0,
        val flagVatAmount: Boolean? = true,
        val finalAmount: Int? = 0,
        val deliveryDate: LocalDateTime? = null,
        val paymentMethod: String? = null,
        val deliveryAddr: String? = null,
        val remark: String? = null,

        val updateDate: LocalDateTime? = null,
        val updateUser: String? = null,
        val createDate: LocalDateTime? = null,
        val createUser: String? = null,
        val flagActive: Boolean? = null
    )

    data class OrderDetailNullableDto(
        val id: Long? = null,
        val site: String?=null,
        val compCd: String? = null,
        val orderNo: String? = null,
        val orderSubNo: String? = null,
        val systemMaterialId: String? = null,
        val materialName: String? = null,
        val materialStandard: String? = null,
        val unit: String? = null,
        val deliveryDate: LocalDate? = null,
        val quantity: Int? = null,
        val unitPrice: Int? = null,
        val supplyPrice: Int? = null,
        val vatPrice: Int? = null,
        val totalPrice: Int? = null,
        val remark: String? = null,

        val updateDate: LocalDateTime? = null,
        val updateUser: String? = null,
        val createDate: LocalDateTime? = null,
        val createUser: String? = null,
        val flagActive: Boolean? = null
    )

    data class OrderHeaderRequest(
        val id: Long? = null,
        val site: String? = null,
        val compCd: String? = null,
        val orderNo: String? = null,
        val orderDate: String,
        val customerId: String,
        val orderer: String? = null,
        val flagVatAmount: Boolean? = true,
        val deliveryDate: String? = null,
        val paymentMethod: String? = null,
        val deliveryAddr: String? = null,
        val remark: String? = null
    )

    data class OrderDetailRequest(
        val id: Long? = null,
        val site: String?=null,
        val compCd: String? = null,
        val orderNo: String? = null,
        val orderSubNo: String? = null,
        val systemMaterialId: String? = null,
        val materialName: String? = null,
        val materialStandard: String? = null,
        val unit: String? = null,
        val deliveryDate: LocalDate? = null,
        val quantity: Int? = null,
        val unitPrice: Int? = null,
        val supplyPrice: Int? = null,
        val vatPrice: Int? = null,
        val totalPrice: Int? = null,
        val remark: String? = null
    )
}