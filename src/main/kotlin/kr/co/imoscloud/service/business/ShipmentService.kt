package kr.co.imoscloud.service.business

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.business.OrderHeader
import kr.co.imoscloud.entity.business.ShipmentDetail
import kr.co.imoscloud.entity.business.ShipmentHeader
import kr.co.imoscloud.repository.business.OrderDetailRepository
import kr.co.imoscloud.repository.business.ShipmentDetailRepository
import kr.co.imoscloud.repository.business.ShipmentHeaderRepository
import kr.co.imoscloud.util.AuthLevel
import kr.co.imoscloud.util.DateUtils
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ShipmentService(
    val headerRepo: ShipmentHeaderRepository,
    private val detailRepo: ShipmentDetailRepository,
    private val orderDetailRepo: OrderDetailRepository,
) {

    fun getHeadersBySearchRequest(req: ShipmentSearchRequest): List<ShipmentHeaderNullableDto> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val (from, to) = DateUtils.getSearchDateRange(req.fromDate, req.toDate)

        return headerRepo.getAllBySearchCondition(
            loginUser.compCd,
            req.orderNo,
            req.customerId,
            req.shipmentStatus,
            from, to
        )
    }

    fun generateHeaderByOrderHeader(header: OrderHeader): ShipmentHeader =
        ShipmentHeader(
            site = header.site,
            compCd = header.compCd,
            orderNo = header.orderNo,
            shipmentStatus = "not",
            shippedQuantity = 0,
            unshippedQuantity = header.orderQuantity
        ).apply {
            createUser = header.createUser
            createDate = header.createDate
            updateUser = header.updateUser
            updateDate = header.updateDate
        }

    fun getDetailsByShipmentId(id: Long): List<ShipmentDetailNullableDto> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return detailRepo.findAllByCompCdAndShipmentIdAndFlagActiveIsTrue(loginUser.compCd, id)
    }

    @AuthLevel(minLevel = 2)
    fun softDeleteByShipmentId(id: Long): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        detailRepo.softDelete(loginUser.getSite(), loginUser.compCd, id, loginUser.loginId)
            .let { if (it==0) throw IllegalArgumentException("삭제할 출하정보가 존재하지 않습니다. ") }

        return "삭제 성공"
    }

    @AuthLevel(minLevel = 2)
    @Transactional
    fun upsertShipmentDetails(list: List<ShipmentDetailRequest>): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val indies = list.mapNotNull { it.id }

        if (detailRepo.existsOlderByMaterialNative(loginUser.compCd, indies))
            throw IllegalArgumentException("각각의 품목별 최신 정보만 편집 기능을 이용할 수 있습니다.")

        val quantityMap: MutableMap<String, Int> = HashMap()
        val detailMap = detailRepo.findAllByCompCdAndIdInAndFlagActiveIsTrue(loginUser.compCd, indies).associateBy { it.id }

        val detailList = list.map { req ->
            val existing = detailMap[req.id]
            existing
                ?.let { detail ->
                    val oldCumulativeShipmentQuantity = detail.cumulativeShipmentQuantity
                    //TODO:: productWarehouseId 로 재고 수량을 조회하고 최대값을 넘는지 유효성 체크 필요

                    detail.apply {
                        shipmentDate = DateUtils.parseDate(req.shipmentDate) ?: this.shipmentDate
                        cumulativeShipmentQuantity = req.cumulativeShipmentQuantity ?: this.cumulativeShipmentQuantity
                        shipmentHandler = req.shipmentHandler ?: this.shipmentHandler
                    }

                    val newQuantity = (oldCumulativeShipmentQuantity?:0)-(req.cumulativeShipmentQuantity?:0)
                    val mapIndex = "${req.shipmentId}-${req.orderNo}"
                    quantityMap[mapIndex] = quantityMap.getOrDefault(mapIndex, 0) + newQuantity
                    detail
                }
                ?:run {
                    val detail = ShipmentDetail(
                        site = req.site ?: loginUser.getSite(),
                        compCd = req.compCd ?: loginUser.compCd,
                        orderNo = req.orderNo!!,
                        orderSubNo = req.orderSubNo!!,
                        systemMaterialId = req.systemMaterialId,
                        shipmentId = req.shipmentId!!,
                        shipmentDate = DateUtils.parseDate(req.shipmentDate),
                        shippedQuantity = req.shippedQuantity,
                        unshippedQuantity = req.unshippedQuantity,
                        stockQuantity = req.stockQuantity,
                        cumulativeShipmentQuantity = req.cumulativeShipmentQuantity,
                        shipmentHandler = req.shipmentHandler,
                        shipmentWarehouse = "제품창고",
                        remark = req.remark
                    )

                    val mapIndex = "${req.shipmentId}-${req.orderNo}"
                    quantityMap[mapIndex] = quantityMap.getOrDefault(mapIndex, 0) + detail.cumulativeShipmentQuantity!!
                    detail
                }
        }

        quantityMap.entries.forEach { (key, value) ->
            val split = key.split("-")
            val shipmentId = split.first()
            val orderNo = split.last()
            headerRepo.updateQuantity(shipmentId, orderNo, value, loginUser.compCd)
        }

        detailRepo.saveAll(detailList)
        return "출하등록 정보 생성 및 수정 성공"
    }

    // 출하등록 정보 하단 그리드의 품목ID 선택 시 나머지 필드에 맵핑해 줄 값을 반환하는 서비스
    fun prepareShipmentDetailsForEntry(orderNo: String): List<ShipmentDetailNullableDto> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val semiShipmentDetails = orderDetailRepo.getAllByOrderNoWithMaterial(loginUser.getSite(), loginUser.compCd, orderNo)
            .groupBy { it.systemMaterialId }.values
            .mapNotNull { values: List<OrderDetailWithMaterialDto> ->
                if (values.isNotEmpty()) {
                    val base = values.first()
                    val totalQuantity: Int = values.sumOf { it.quantity ?: 0 }

                    ShipmentDetailNullableDto(
                        site = loginUser.getSite(),
                        compCd = loginUser.compCd,
                        orderNo = base.orderNo,
                        systemMaterialId = base.systemMaterialId,
                        materialName = base.materialName,
                        materialStandard = base.materialStandard,
                        unit = base.unit,
                        quantity = totalQuantity
                    )
                } else null
            }

        val shipmentDetailMap = detailRepo.getAllByOrderNo(loginUser.getSite(), loginUser.compCd, orderNo)
            .associateBy { it.systemMaterialId }

        return semiShipmentDetails.map { semi ->
            val systemMaterialId = semi.systemMaterialId!!
             shipmentDetailMap[systemMaterialId]
                ?.let { base ->
                    semi.apply {
                        stockQuantity = 100
                        shipmentId = base.shipmentId
                        shipmentDate = base.shipmentDate
                        shippedQuantity = (base.shippedQuantity?: 0)+(base.cumulativeShipmentQuantity?: 0)
                        unshippedQuantity = (base.unshippedQuantity?: 0)-(base.cumulativeShipmentQuantity?: 0)
                    }
                }
                ?: semi.apply {
                    stockQuantity = 100
                    shippedQuantity = this.quantity
                    unshippedQuantity = 0
                }
        }
    }
}

data class ShipmentHeaderNullableDto(
    val id: Long? = null,
    val site: String? = null,
    val compCd: String? = null,
    val orderNo: String? = null,
    //OrderHeader
    val orderDate: LocalDate? = null,
    val orderer: String? = null,
    val orderQuantity: Int? = null,
    val customerId: String? = null,
    val totalAmount: Int? = 0,
    //ShipmentHeader
    val shipmentStatus: String?=null,
    val shippedQuantity: Int? = null,
    val unshippedQuantity: Int? = null,
    val remark: String? = null,
)

data class ShipmentDetailNullableDto(
    val id: Long? = null,
    val site: String? = null,
    val compCd: String? = null,
    val orderNo: String? = null,
    var orderSubNo: String? = null,
    //OrderDetail
    val systemMaterialId: String? = null,
    val materialName: String? = null,
    val materialStandard: String? = null,
    val unit: String? = null,
    var quantity: Int? = null,
    //Warehouse
    var stockQuantity: Int? = null,
    //ShipmentHeader
    var shipmentId: Long? = null,
    var shipmentDate: LocalDate? = null,
    var shippedQuantity: Int? = null,
    var unshippedQuantity: Int? = null,
    var cumulativeShipmentQuantity: Int? = null,
    var shipmentWarehouse: String? = "제품창고",
    var shipmentHandler: String? = null,
    var remark: String? = null,
    var flagPrint: Boolean? = false
)

data class ShipmentSearchRequest(
    val orderNo: String?=null,
    val fromDate: String?=null,
    val toDate: String?=null,
    val customerId: String?=null,
    val shipmentStatus: String?=null,
)

data class ShipmentDetailRequest(
    val id: Long? = null,
    val site: String? = null,
    val compCd: String? = null,
    val orderNo: String? = null,
    var orderSubNo: String? = null,
    //OrderDetail
    val systemMaterialId: String? = null,
    val materialName: String? = null,
    val materialStandard: String? = null,
    val unit: String? = null,
    var quantity: Int? = null,
    //Warehouse
    var stockQuantity: Int? = null,
    //ShipmentHeader
    var shipmentId: Long? = null,
    var shipmentDate: String? = null,
    var shippedQuantity: Int? = null,
    var unshippedQuantity: Int? = null,
    var cumulativeShipmentQuantity: Int? = null,
    var shipmentWarehouse: String? = "제품창고",
    var shipmentHandler: String? = null,
    var remark: String? = null,
)

data class OrderDetailWithMaterialDto(
    val orderNo: String? = null,
    val systemMaterialId: String? = null,
    val materialName: String? = null,
    val materialStandard: String? = null,
    val unit: String? = null,
    var quantity: Int? = null,
)