package kr.co.imoscloud.service.business

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.business.OrderDetail
import kr.co.imoscloud.entity.business.OrderHeader
import kr.co.imoscloud.entity.material.MaterialMaster
import kr.co.imoscloud.repository.CodeRep
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
    val detailRepo: OrderDetailRepository,
    private val materialRepo: MaterialRepository,
    private val codeRep: CodeRep
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

    fun addHeader(no: Int): OrderHeaderNullableDto {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val latestNo = headerRepo.getLatestOrderNo(loginUser.compCd)

        val nextVersion = latestNo
            ?.takeLast(3)
            ?.trim()
            ?.toIntOrNull()
            ?.plus(no)
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

        headerRepo.deleteOrderHeader(loginUser.getSite(), loginUser.compCd, id, loginUser.loginId)
            .let { if (it == 0) throw IllegalArgumentException("기본 주문정보가 존재하지 않습니다. ") }
        detailRepo.deleteAllByOrderHeaderId(loginUser.getSite(), loginUser.compCd, id, loginUser.loginId)
            .let { if (it == 0) throw IllegalArgumentException("주문상세정보가 존재하지 않습니다. ") }

        return "삭제 성공"
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
                    orderDate = DateUtils.parseDate(req.orderDate) ?: this.orderDate
                    customerId = req.customerId ?: this.customerId
                    ordererId = req.orderer ?: this.ordererId
                    flagVatAmount = req.flagVatAmount ?: this.flagVatAmount
                    deliveryDate = DateUtils.parseDateTime(req.deliveryDate) ?: this.deliveryDate
                    paymentMethod = req.paymentMethod ?: this.paymentMethod
                    deliveryAddr = req.deliveryAddr ?: this.deliveryAddr
                    remark = req.remark ?: this.remark
                    updateCommonCol(loginUser)
                }
                ?:run {
                    try {
                        OrderHeader(
                            site = req.site!!,
                            compCd = req.compCd!!,
                            orderNo = req.orderNo!!,
                            orderDate = DateUtils.parseDate(req.orderDate),
                            customerId = req.customerId!!,
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

    fun addDetail(req: NewDetailRequest): OrderDetailNullableDto {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val latestSubOrderNo = detailRepo.getLatestOrderSubNo(loginUser.compCd)
        val nextVersion = latestSubOrderNo
            ?.takeLast(3)
            ?.trim()
            ?.toIntOrNull()
            ?.plus(1)
            ?.let { "%03d".format(it) }
            ?: "001"

        return OrderDetailNullableDto(
            site = loginUser.getSite(),
            compCd = loginUser.compCd,
            orderNo = req.orderNo,
            orderSubNo = nextVersion,
        )
    }

    @Transactional
    fun deleteDetail(id: Long): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val detail = detailRepo.findBySiteAndCompCdAndIdAndFlagActiveIsTrue(loginUser.getSite(), loginUser.compCd, id)
            ?.let {
                val deleteIt = it.apply { flagActive = false;updateCommonCol(loginUser) }

                val totalPrice = deleteIt.totalPrice?.let { p -> p * -1 } ?:0
                val vatPrice = deleteIt.vatPrice?.let { p -> p * -1 } ?:0
                if (totalPrice != 0 || vatPrice != 0) {
                    headerRepo.updateAmountsByDetailPrice(deleteIt.orderNo, totalPrice, vatPrice)
                }
                deleteIt
            }
            ?: throw IllegalArgumentException("주문상세정보가 존재하지 않습니다. ")

        detailRepo.save(detail)
        return "${detail.orderNo} - ${detail.orderSubNo} 삭제 성공"
    }

    @Transactional
    fun upsertDetails(list: List<OrderDetailRequest>): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val indies = list.mapNotNull { it.id }
        val detailMap = detailRepo
            .findAllBySiteAndCompCdAndIdInAndFlagActiveIsTrue(loginUser.getSite(), loginUser.compCd, indies)
            .associateBy { it.id }

        val systemVat= codeRep.getInitialCodes("VAT").first()?.codeName?.split(" ")?.first()?.toInt()?:10

        val amountMap: MutableMap<String, TotalPriceWithVat> = mutableMapOf()
        val detailList: List<OrderDetail> = list.map { req ->
            detailMap[req.id]
                ?.let { detail ->
                    val oldPrice = TotalPriceWithVat(detail.vatPrice!!, detail.totalPrice!!)

                    var modifyDetail = detail.apply {
                        supplyPrice = req.supplyPrice ?: this.supplyPrice
                        discountedAmount = req.discountedAmount ?: this.discountedAmount
                        systemMaterialId = req.systemMaterialId ?: this.systemMaterialId
                        deliveryDate = DateUtils.parseDate(req.deliveryDate) ?: this.deliveryDate
                        quantity = req.quantity ?: this.quantity
                        unitPrice = req.unitPrice ?: this.unitPrice
                        supplyPrice = req.supplyPrice ?: this.supplyPrice
                        remark = req.remark ?: this.remark
                        updateCommonCol(loginUser)
                    }

                    modifyDetail = calculatePrice(modifyDetail, systemVat)
                    val newVat = oldPrice.vat - modifyDetail.vatPrice!!
                    val newTotal = oldPrice.total - modifyDetail.totalPrice!!
                    val existsPrice = amountMap[detail.orderNo] ?: TotalPriceWithVat()
                    amountMap[detail.orderNo] = TotalPriceWithVat(existsPrice.total + newTotal, existsPrice.vat + newVat)
                    modifyDetail
                }
                ?:run {
                    var detail = OrderDetail(
                        site = req.site ?: loginUser.getSite(),
                        compCd = req.compCd ?: loginUser.compCd,
                        orderNo = req.orderNo!!,
                        orderSubNo = req.orderSubNo!!,
                        systemMaterialId = req.systemMaterialId,
                        deliveryDate = DateUtils.parseDate(req.deliveryDate),
                        quantity = req.quantity!!,
                        unitPrice = req.unitPrice!!,
                        discountedAmount = req.discountedAmount,
                        supplyPrice = req.supplyPrice,
                        remark = req.remark,
                    ).apply { createCommonCol(loginUser) }

                    detail = calculatePrice(detail, systemVat)
                    val existsPrice = amountMap[detail.orderNo] ?: TotalPriceWithVat()
                    val finalTotal = existsPrice.total + detail.totalPrice!!
                    val finalVat = existsPrice.vat + detail.vatPrice!!
                    amountMap[detail.orderNo] = TotalPriceWithVat(finalTotal, finalVat)
                    detail
                }
        }

        detailRepo.saveAll(detailList)
        amountMap.entries.forEach { (key, value) ->
            headerRepo.updateAmountsByDetailPrice(key, value.total, value.vat)
                .let { if (it == 0) throw IllegalArgumentException("기본 주문정보가 존재하지 않습니다. ") }
        }
        return "주문상세정보 생성 및 수정 성공"
    }

    private fun calculatePrice(modifyDetail: OrderDetail, systemVat: Int): OrderDetail {
        var modifyDetail1 = modifyDetail
        modifyDetail1 = modifyDetail1.apply {
            vatPrice = supplyPrice?.let { it / systemVat } ?: run { (unitPrice * quantity) / systemVat }
            totalPrice = supplyPrice
                ?.let { it - (this.discountedAmount ?: 0) }
                ?: run { (unitPrice * quantity) - (this.discountedAmount ?: 0) }
        }
        return modifyDetail1
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
        val orderDate: String? = null,
        val customerId: String? = null,
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
        val deliveryDate: String? = null,
        val quantity: Int? = null,
        val unitPrice: Int? = null,
        val supplyPrice: Int? = null,
        val discountedAmount: Int? = null,
        val remark: String? = null
    )

    data class TotalPriceWithVat(
        var total: Int = 0,
        var vat: Int = 0
    )

    data class NewDetailRequest(
        val no: Int,
        val orderNo: String
    )
}