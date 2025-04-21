package kr.co.imoscloud.service.business

import kr.co.imoscloud.repository.business.ShipmentDetailRepository
import kr.co.imoscloud.repository.business.ShipmentHeaderRepository
import kr.co.imoscloud.util.DateUtils
import kr.co.imoscloud.util.SecurityUtils
import org.apache.catalina.security.SecurityUtil
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ShipmentService(
    private val headerRepo: ShipmentHeaderRepository,
    private val detailRepo: ShipmentDetailRepository
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
    val stockQuantity: Int? = null,
    //ShipmentHeader
    var shipmentId: Long? = null,
    var shipmentDate: LocalDate? = null,
    var shippedQuantity: Int? = null,
    var unshippedQuantity: Int? = null,
    var cumulativeShipmentQuantity: Int? = null,
    var shipmentWarehouse: String? = "제품창고",
    var shipmentHandler: String? = null,
    var remark: String? = null
)

data class ShipmentSearchRequest(
    val orderNo: String?=null,
    val fromDate: String?=null,
    val toDate: String?=null,
    val customerId: String?=null,
    val shipmentStatus: String?=null,
)