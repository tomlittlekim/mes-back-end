package kr.co.imoscloud.service.business

import kr.co.imoscloud.entity.business.OrderDetail
import kr.co.imoscloud.entity.business.OrderHeader
import kr.co.imoscloud.repository.business.OrderDetailRepository
import kr.co.imoscloud.repository.business.OrderHeaderRepository
import kr.co.imoscloud.util.DateUtils
import org.springframework.stereotype.Service

@Service
class OrderService(
    val headerRepo: OrderHeaderRepository,
    val detailRepo: OrderDetailRepository
) {

    // orderHeader 조회
    fun getAllHeaderBySearchRequestByCompCd(req: OrderHeaderSearchRequest): List<OrderHeader> {
        val (from, to) = DateUtils.getSearchDateRange(req.fromDate, req.toDate)

        return req.materialId
            ?.let { detailRepo.findAllBySearchCondition(req.orderNo, from, to, req.customerId, req.materialId) }
            ?:run { headerRepo.findAllBySearchCondition(req.orderNo, from, to, req.customerId) }
    }

    data class OrderHeaderSearchRequest(
        val orderNo: String?=null,
        val fromDate: String?=null,
        val toDate: String?=null,
        val customerId: String?=null,
        val materialId: String?=null,
    )

    data class OrderResponse(
        val header: List<OrderHeader> = listOf(),
        val details: List<OrderDetail>? = emptyList()
    )
}