package kr.co.imoscloud.fetcher

import com.netflix.graphql.dgs.*
import kr.co.imoscloud.service.business.OrderService
import kr.co.imoscloud.service.business.ShipmentService

@DgsComponent
class BusinessFetcher(
    private val orderService: OrderService,
    private val shipmentService: ShipmentService
) {

    /** 주문헤더 검색 */
    @DgsQuery
    fun orderHeaders(@InputArgument req: OrderService.OrderHeaderSearchRequest): List<OrderService.OrderHeaderNullableDto> {
        return orderService.getHeadersBySearchRequestByCompCd(req)
    }

    /** 주문헤더 단건 생성용 기본값 반환 */
    @DgsQuery
    fun newOrderHeader(): OrderService.OrderHeaderNullableDto {
        return orderService.addHeader()
    }

    /** 주문상세 단건 생성용 기본값 반환 */
    @DgsQuery
    fun newOrderDetail(@InputArgument req: OrderService.OrderHeaderNullableDto): OrderService.OrderDetailNullableDto {
        return orderService.addDetail(req)
    }

    /** 주문상세 목록 조회 */
    @DgsQuery
    fun orderDetails(@InputArgument orderNo: String): List<OrderService.OrderDetailNullableDto> {
        return orderService.getDetailsByOrderNo(orderNo)
    }

    /** 주문헤더 저장/수정 */
    @DgsMutation
    fun upsertOrderHeaders(@InputArgument list: List<OrderService.OrderHeaderRequest>): String {
        return orderService.upsertHeader(list)
    }

    /** 주문상세 저장/수정 */
    @DgsMutation
    fun upsertOrderDetails(@InputArgument list: List<OrderService.OrderDetailRequest>): String {
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
}