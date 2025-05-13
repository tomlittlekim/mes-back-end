package kr.co.imoscloud.service.business

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.business.OrderHeader
import kr.co.imoscloud.entity.business.ShipmentDetail
import kr.co.imoscloud.entity.business.ShipmentHeader
import kr.co.imoscloud.entity.business.TransactionStatementDetail
import kr.co.imoscloud.entity.standardInfo.Warehouse
import kr.co.imoscloud.repository.business.OrderDetailRepository
import kr.co.imoscloud.repository.business.ShipmentDetailRepository
import kr.co.imoscloud.repository.business.ShipmentHeaderRepository
import kr.co.imoscloud.repository.business.TransactionStatementDetailRepository
import kr.co.imoscloud.repository.inventory.InventoryStatusRep
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
    private val inventoryStatusRep: InventoryStatusRep,
    private val tsdRepo: TransactionStatementDetailRepository
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

    fun generateShipmentHeader(header: OrderHeader): ShipmentHeader =
        ShipmentHeader(
            site = header.site,
            compCd = header.compCd,
            orderNo = header.orderNo,
            shipmentStatus = "not",
            shippedQuantity = 0.0,
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
    @Transactional
    fun softDeleteByShipmentId(id: Long): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        detailRepo.softDelete(loginUser.getSite(), loginUser.compCd, id, loginUser.loginId)
            .let { if (it==0) throw IllegalArgumentException("삭제할 출하정보가 존재하지 않습니다. ") }

        detailRepo.softDeleteToTSD(
            loginUser.getSite(),
            loginUser.compCd,
            id,
            loginUser.loginId
        )
        return "삭제 성공"
    }

    @AuthLevel(minLevel = 2)
    @Transactional
    fun cancelShipment(orderNo: String): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val header = headerRepo
            .findBySiteAndCompCdAndOrderNoAndFlagActiveIsTrue(loginUser.getSite(), loginUser.compCd, orderNo)
            ?.let { it.apply { flagActive = false; updateCommonCol(loginUser) } }
            ?: throw IllegalArgumentException("출하정보가 존재하지 않습니다. ")
        headerRepo.save(header)

        val details = detailRepo.findAllByShipmentIdAndFlagActiveIsTrueOrderByCreateDate(header.shipmentId!!)
            .map { it.apply { flagActive = false; updateCommonCol(loginUser) } }

        if (details.isNotEmpty()) {
            val latest = details.last()
            inventoryStatusRep.updateQtyByIdAndSystemMaterialId(
                loginUser.compCd,
                latest.shipmentWarehouse!!,
                latest.systemMaterialId!!,
                latest.cumulativeShipmentQuantity ?: 0.0,
                loginUser.loginId
            )

            detailRepo.saveAll(details)
        }

        return "삭제 성공"
    }

    @AuthLevel(minLevel = 2)
    @Transactional
    fun upsertShipmentDetails(list: List<ShipmentDetailRequest>): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val indies = list.mapNotNull { it.id }

        if (detailRepo.existsOlderByMaterialNative(loginUser.compCd, indies) > 0)
            throw IllegalArgumentException("각각의 품목별 최신 정보만 편집 기능을 이용할 수 있습니다.")

        val quantityMap: MutableMap<String, Double> = HashMap()
        val inventoryMap: MutableMap<String, Double> = HashMap()
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
                        updateCommonCol(loginUser)
                    }

                    val newQty = (req.cumulativeShipmentQuantity?:0.0)-(oldCumulativeShipmentQuantity?:0.0)
                    var mapIndex = "${req.shipmentId}-${req.orderNo}"
                    quantityMap[mapIndex] = quantityMap.getOrDefault(mapIndex, 0.0) + newQty

                    mapIndex = "${req.shipmentWarehouse}_${req.systemMaterialId}"
                    inventoryMap[mapIndex] = inventoryMap.getOrDefault(mapIndex, 0.0) + (newQty * -1.0)
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
//                        stockQuantity = req.stockQuantity, 재고수량
                        cumulativeShipmentQuantity = req.cumulativeShipmentQuantity,
                        shipmentHandler = req.shipmentHandler,
                        shipmentWarehouse = req.shipmentWarehouse,
                        remark = req.remark
                    ).apply { createCommonCol(loginUser) }

                    var mapIndex = "${req.shipmentId}-${req.orderNo}"
                    val cumulativeQty = detail.cumulativeShipmentQuantity!!
                    quantityMap[mapIndex] = quantityMap.getOrDefault(mapIndex, 0.0) + cumulativeQty

                    mapIndex = "${req.shipmentWarehouse}_${req.systemMaterialId}"
                    inventoryMap[mapIndex] = inventoryMap.getOrDefault(mapIndex, 0.0) - cumulativeQty

                    detail
                }
        }

        quantityMap.entries.forEach { (key, value) ->
            val split = key.split("-")
            val shipmentId = split.first()
            val orderNo = split.last()
            headerRepo.updateQuantity(shipmentId, orderNo, value, loginUser.loginId)
        }

        inventoryMap.entries.forEach { (key, value) ->
            val split = key.split("_")
            val warehouseId = split.first()
            val systemMaterialId = split.last()
            inventoryStatusRep.updateQtyByIdAndSystemMaterialId(
                loginUser.compCd,
                warehouseId,
                systemMaterialId,
                value,
                loginUser.loginId
            )
        }

        val details: List<ShipmentDetail> = detailRepo.saveAll(detailList)
        val tsdList = details.map { generateTransactionStatementDetail(it) }
        if (tsdList.isNotEmpty()) tsdRepo.saveAll(tsdList)

        return "출하등록 정보 생성 및 수정 성공"
    }

    // 출하등록 정보 하단 그리드의 품목ID 선택 시 나머지 필드에 맵핑해 줄 값을 반환하는 서비스
    fun prepareShipmentDetailsForEntry(req: ShipmentDetailEntryRequest): ShipmentDetailNullableDto {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val semiShipmentDetail = orderDetailRepo.getInitialByRequest(
            loginUser.getSite(),
            loginUser.compCd,
            req.orderNo,
            req.orderSubNo,
            req.warehouseId
        ) ?: throw IllegalArgumentException("품목ID 에 대한 주문이 존재하지 않습니다. ")

        return detailRepo.getLatestDetailByRequest(
            loginUser.getSite(),
            loginUser.compCd,
            req.orderNo,
            req.orderSubNo,
            semiShipmentDetail.systemMaterialId!!
        )
            ?.let { latest ->
                semiShipmentDetail.apply {
                    shipmentId = latest.shipmentId
                    shipmentDate = latest.shipmentDate
                    shippedQuantity = (latest.shippedQuantity?: 0.0)+(latest.cumulativeShipmentQuantity?: 0.0)
                    unshippedQuantity = (latest.unshippedQuantity?: 0.0)-(latest.cumulativeShipmentQuantity?: 0.0)
                }
            }
            ?:run {
                semiShipmentDetail.apply {
                    shippedQuantity = 0.0
                    unshippedQuantity = this.quantity
                }
            }
    }

    fun getMaterialByOrderNo(orderNo: String): List<MaterialWithOrderDetail> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return detailRepo.getMaterialsByOrderNo(loginUser.getSite(), loginUser.compCd, orderNo)
    }

    fun getWarehouseByMaterialId(materialId: String): List<Warehouse> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return inventoryStatusRep.getWarehouseByMaterialId(loginUser.getSite(), loginUser.compCd, materialId)
    }

    private fun generateTransactionStatementDetail(shipmentDetail: ShipmentDetail): TransactionStatementDetail {
        return TransactionStatementDetail(
            site = shipmentDetail.site,
            compCd = shipmentDetail.compCd,
            orderNo = shipmentDetail.orderNo,
            orderSubNo = shipmentDetail.orderSubNo,
            shipmentDetailId = shipmentDetail.id!!,
            shipmentDate = shipmentDetail.shipmentDate,
        ).apply {
            createUser = shipmentDetail.createUser
            createDate = shipmentDetail.createDate
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
    val orderQuantity: Double? = null,
    val customerId: String? = null,
    val totalAmount: Int? = 0,
    //ShipmentHeader
    val shipmentStatus: String?=null,
    val shippedQuantity: Double? = null,
    val unshippedQuantity: Double? = null,
    val remark: String? = null,
    var flagPrint: Boolean? = false
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
    var quantity: Double? = null,
    //Warehouse
    var stockQuantity: Double? = null,
    //ShipmentHeader
    var shipmentId: Long? = null,
    var shipmentDate: LocalDate? = null,
    var shippedQuantity: Double? = null,
    var unshippedQuantity: Double? = null,
    var cumulativeShipmentQuantity: Double? = null,
    var shipmentWarehouse: String? = null,
    var shipmentHandler: String? = null,
    var remark: String? = null,
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
    var quantity: Double? = null,
    //Warehouse
    var stockQuantity: Double? = null,
    //ShipmentHeader
    var shipmentId: Long? = null,
    var shipmentDate: String? = null,
    var shippedQuantity: Double? = null,
    var unshippedQuantity: Double? = null,
    var cumulativeShipmentQuantity: Double? = null,
    var shipmentWarehouse: String? = null,
    var shipmentHandler: String? = null,
    var remark: String? = null,
)

data class ShipmentDetailEntryRequest(
    val orderNo: String,
    val orderSubNo: String,
    val warehouseId: String
)

data class MaterialWithOrderDetail(
    val systemMaterialId: String,
    val materialName: String,
    val orderSubNo: String
)