package kr.co.imoscloud.service.business

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.business.OrderDetail
import kr.co.imoscloud.entity.business.OrderHeader
import kr.co.imoscloud.entity.business.ShipmentHeader
import kr.co.imoscloud.entity.business.TransactionStatement
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
    private val codeRep: CodeRep,
    private val shipmentService: ShipmentService,
    private val transactionStatementService: TransactionStatementService
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
            ?: "%03d".format(no)

        val prefix = "${loginUser.compCd}${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))}"

        return OrderHeaderNullableDto(
            site = loginUser.getSite(),
            compCd = loginUser.compCd,
            orderNo = "$prefix$nextVersion",
        )
    }

    @Transactional
    fun deleteHeader(id: Long): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        headerRepo.deleteOrderHeader(loginUser.getSite(), loginUser.compCd, id, loginUser.loginId)
            .let { if (it == 0) throw IllegalArgumentException("기본 주문정보가 존재하지 않습니다. ") }
        detailRepo.deleteAllByOrderHeaderId(loginUser.getSite(), loginUser.compCd, id, loginUser.loginId)

        return "삭제 성공"
    }

    @Transactional
    fun upsertHeader(list: List<OrderHeaderRequest>): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val indies = list.mapNotNull { it.id }
        val headerMap = headerRepo
            .findAllBySiteAndCompCdAndIdInAndFlagActiveIsTrue(loginUser.getSite(), loginUser.compCd, indies)
            .associateBy { it.id }

        val shipmentHeaders: MutableList<ShipmentHeader> = mutableListOf()
        val statements: MutableList<TransactionStatement> = mutableListOf()

        val headerList: List<OrderHeader> = list.map { req ->
            headerMap[req.id]
                ?.apply {
                    orderDate = DateUtils.parseDate(req.orderDate) ?: this.orderDate
                    customerId = req.customerId ?: this.customerId
                    ordererId = req.orderer ?: this.ordererId
                    flagVatAmount = req.flagVatAmount ?: this.flagVatAmount
                    deliveryDate = DateUtils.parseDate(req.deliveryDate) ?: this.deliveryDate
                    paymentMethod = req.paymentMethod ?: this.paymentMethod
                    deliveryAddr = req.deliveryAddr ?: this.deliveryAddr
                    remark = req.remark ?: this.remark
                    updateCommonCol(loginUser)
                }
                ?:run {
                    try {
                        val orderHeader = OrderHeader(
                            site = req.site!!,
                            compCd = req.compCd!!,
                            orderNo = req.orderNo!!,
                            orderDate = DateUtils.parseDate(req.orderDate),
                            ordererId = req.orderer,
                            customerId = req.customerId!!,
                            flagVatAmount = req.flagVatAmount!!,
                            deliveryDate = DateUtils.parseDate(req.deliveryDate),
                            paymentMethod = req.paymentMethod,
                            deliveryAddr = req.deliveryAddr,
                            remark = req.remark,
                        ).apply { createCommonCol(loginUser) }

                        shipmentHeaders.add(shipmentService.generateShipmentHeader(orderHeader))
                        statements.add(transactionStatementService.generateTransactionHeader(orderHeader))
                        orderHeader
                    } catch (e: NullPointerException) {
                        throw IllegalArgumentException("기본 주문정보를 생성할 필드값이 부족합니다. ")
                    }
                }
            }

        if (shipmentHeaders.isNotEmpty()) shipmentService.headerRepo.saveAll(shipmentHeaders)
        if (statements.isNotEmpty()) transactionStatementService.headerRepo.saveAll(statements)

        headerRepo.saveAll(headerList)
        return "기본 주문정보 생성 및 수정 성공"
    }



    fun getDetailsByOrderNo(orderNo: String): List<OrderDetailNullableDto> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val detailList = detailRepo.findAllByCompCdAndOrderNoAndFlagActiveIsTrue(loginUser.compCd, orderNo)

        val indies = detailList.map { it.systemMaterialId }
        val materialMap = materialRepo.getMaterialListByIds(loginUser.getSite(), loginUser.compCd, indies)
            .associateBy { it?.systemMaterialId }

        return detailList.map { detailToResponse(it, materialMap) }
    }

    fun addDetail(req: NewDetailRequest): OrderDetailNullableDto {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val latestSubOrderNo = detailRepo.getLatestOrderSubNo(loginUser.compCd, req.orderNo)
        val nextVersion = latestSubOrderNo
            ?.toIntOrNull()
            ?.plus(req.no)
            ?.let { "%03d".format(it) }
            ?: "%03d".format(req.no)

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
                    headerRepo.updateAmountsByDetailPrice(deleteIt.orderNo, totalPrice, vatPrice, (deleteIt.quantity*-1.0))
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

        val amountMap: MutableMap<String, CalculateFroOrder> = mutableMapOf()
        val detailList: List<OrderDetail> = list.map { req ->
            val existing = detailMap[req.id]
            existing
                ?.let { detail ->
                    val oldSupplyPrice = existing.supplyPrice ?: 0
                    val oldVat = existing.vatPrice ?: 0
                    val oldQty = existing.quantity

                    detail.apply {
                        discountedAmount = req.discountedAmount ?: this.discountedAmount
                        systemMaterialId = req.systemMaterialId ?: this.systemMaterialId
                        deliveryDate = DateUtils.parseDate(req.deliveryDate) ?: this.deliveryDate
                        quantity = req.quantity ?: this.quantity
                        unitPrice = req.unitPrice ?: this.unitPrice
                        supplyPrice = (if(req.quantity != null || req.unitPrice != null) {
                            (req.quantity ?: this.quantity) * (req.unitPrice ?: this.unitPrice)
                        } else this.supplyPrice) as Int?
                        remark = req.remark ?: this.remark
                        updateCommonCol(loginUser)
                    }

                    calculatePrice(req.flagVatAmount, detail, systemVat)
                    val deltaTotal = (detail.supplyPrice ?: 0) - oldSupplyPrice
                    val deltaVat = (detail.vatPrice ?: 0) - oldVat
                    val deltaQty = detail.quantity - oldQty

                    amountMap[detail.orderNo] = amountMap.getOrDefault(detail.orderNo, CalculateFroOrder())
                        .apply {
                            total += deltaTotal
                            vat += deltaVat
                            quantity += deltaQty
                        }
                    detail
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
                        supplyPrice = ((req.quantity * req.unitPrice).toInt()),
                        remark = req.remark,
                    ).apply { createCommonCol(loginUser) }

                    detail = calculatePrice(req.flagVatAmount, detail, systemVat)
                    val existsPrice = amountMap[detail.orderNo] ?: CalculateFroOrder()
                    val finalTotal = existsPrice.total + detail.supplyPrice!!
                    val finalVat = existsPrice.vat + detail.vatPrice!!
                    amountMap[detail.orderNo] = CalculateFroOrder(finalTotal, finalVat, detail.quantity)
                    detail
                }
        }

        detailRepo.saveAll(detailList)
        amountMap.entries.forEach { (key, value) ->
            headerRepo.updateAmountsByDetailPrice(key, value.total, value.vat, value.quantity)
                .let { if (it == 0) throw IllegalArgumentException("기본 주문정보가 존재하지 않습니다. ") }
        }
        return "주문상세정보 생성 및 수정 성공"
    }

    private fun calculatePrice(flagVatAmount: Boolean?, modifyDetail: OrderDetail, systemVat: Int): OrderDetail =
        modifyDetail.apply {
            val vat = if (flagVatAmount != false) this.supplyPrice!!/systemVat else 0
            vatPrice = vat
            totalPrice = this.supplyPrice!! + vat
        }

    private fun headerToResponse(header: OrderHeader): OrderHeaderNullableDto = OrderHeaderNullableDto(
        id = header.id,
        site = header.site,
        compCd = header.compCd,
        orderNo = header.orderNo,
        orderer = header.ordererId,
        orderDate = header.orderDate,
        orderQuantity = header.orderQuantity,
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
    val orderer: String? = null,
    val orderQuantity: Double? = null,
    val customerId: String? = null,
    val totalAmount: Int? = 0,
    val vatAmount: Int? = 0,
    val flagVatAmount: Boolean? = true,
    val finalAmount: Int? = 0,
    val deliveryDate: LocalDate? = null,
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
    val quantity: Double? = null,
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
    val flagVatAmount: Boolean? = false,
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
    val quantity: Double? = null,
    val unitPrice: Int? = null,
    val discountedAmount: Int? = null,
    val remark: String? = null,
    val flagVatAmount: Boolean? = null
)
data class CalculateFroOrder(
    var total: Int = 0,
    var vat: Int = 0,
    var quantity: Double = 0.0,
)

data class NewDetailRequest(
    val no: Int,
    val orderNo: String
)