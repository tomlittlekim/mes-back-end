package kr.co.imoscloud.fetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.entity.standardInfo.Warehouse
import kr.co.imoscloud.service.business.*

@DgsComponent
class BusinessFetcher(
    private val orderService: OrderService,
    private val shipmentService: ShipmentService,
    private val transactionStatementService: TransactionStatementService,
) {

    /** 주문헤더 검색 */
    @DgsQuery
    fun orderHeaders(@InputArgument req: OrderHeaderSearchRequest): List<OrderHeaderNullableDto> {
        return orderService.getHeadersBySearchRequestByCompCd(req)
    }

    /** 주문헤더 단건 생성용 기본값 반환 */
    @DgsQuery
    fun newOrderHeader(@InputArgument no: Int): OrderHeaderNullableDto {
        return orderService.addHeader(no)
    }

    /** 주문상세 단건 생성용 기본값 반환 */
    @DgsQuery
    fun newOrderDetail(@InputArgument req: NewDetailRequest): OrderDetailNullableDto {
        return orderService.addDetail(req)
    }

    /** 주문상세 목록 조회 */
    @DgsQuery
    fun orderDetails(@InputArgument orderNo: String): List<OrderDetailNullableDto> {
        return orderService.getDetailsByOrderNo(orderNo)
    }

    /** 주문헤더 저장/수정 */
    @DgsMutation
    fun upsertOrderHeaders(@InputArgument list: List<OrderHeaderRequest>): String {
        return orderService.upsertHeader(list)
    }

    /** 주문상세 저장/수정 */
    @DgsMutation
    fun upsertOrderDetails(@InputArgument list: List<OrderDetailRequest>): String {
        return orderService.upsertDetails(list)
    }

    /** 주문헤더 삭제 */
    @DgsMutation
    fun deleteOrderHeader(@InputArgument id: Long): String {
        return orderService.deleteHeader(id)
    }

    /** 주문상세 삭제 */
    @DgsMutation
    fun deleteOrderDetail(@InputArgument id: Long): String {
        return orderService.deleteDetail(id)
    }





    @DgsQuery
    fun getShipmentHeaders(@InputArgument req: ShipmentSearchRequest): List<ShipmentHeaderNullableDto> {
        return shipmentService.getHeadersBySearchRequest(req)
    }

    @DgsQuery
    fun getShipmentDetails(@InputArgument id: Long): List<ShipmentDetailNullableDto> {
        return shipmentService.getDetailsByShipmentId(id)
    }

    @DgsQuery
    fun prepareShipmentDetailsForEntry(@InputArgument req: ShipmentDetailEntryRequest): ShipmentDetailNullableDto {
        return shipmentService.prepareShipmentDetailsForEntry(req)
    }

    @DgsMutation
    fun upsertShipmentDetails(@InputArgument list: List<ShipmentDetailRequest>): String {
        return shipmentService.upsertShipmentDetails(list)
    }

    @DgsMutation
    fun softDeleteShipment(@InputArgument shipmentId: Long): String {
        return shipmentService.softDeleteByShipmentId(shipmentId)
    }

    @DgsQuery
    fun getMaterialByOrderNo(@InputArgument orderNo: String): List<MaterialWithOrderDetail> {
        return shipmentService.getMaterialByOrderNo(orderNo)
    }

    @DgsQuery
    fun getWarehouseByMaterialId(@InputArgument materialId: String): List<Warehouse> {
        return shipmentService.getWarehouseByMaterialId(materialId)
    }





    @DgsQuery
    fun transactionStatementHeaders(@InputArgument req: TransactionStatementSearchCondition): List<TransactionStatementHeaderNullableDto> {
        return transactionStatementService.getAllBySearchCondition(req)
    }

    @DgsQuery
    fun transactionStatementDetails(@InputArgument orderNo: String): List<TransactionStatementDetailNullableDto> {
        return transactionStatementService.getAllDetailsByOrderNo(orderNo)
    }

    @DgsMutation
    fun deleteTransactionStatement(@InputArgument orderNo: String): String {
        return transactionStatementService.softDeleteByOrderNo(orderNo)
    }
}