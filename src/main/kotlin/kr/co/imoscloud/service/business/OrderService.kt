package kr.co.imoscloud.service.business

import kr.co.imoscloud.entity.business.OrderDetail
import kr.co.imoscloud.entity.business.OrderHeader
import kr.co.imoscloud.repository.business.OrderDetailRepository
import kr.co.imoscloud.repository.business.OrderHeaderRepository
import kr.co.imoscloud.util.DateUtils
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service

@Service
class OrderService(
    val headerRepo: OrderHeaderRepository,
    val detailRepo: OrderDetailRepository
) {

    // orderHeader 조회
    fun getHeadersBySearchRequestByCompCd(req: OrderHeaderSearchRequest): List<OrderHeader> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val (from, to) = DateUtils.getSearchDateRange(req.fromDate, req.toDate)

        return req.materialId
            ?.let { detailRepo.findAllBySearchCondition(loginUser.compCd, req.orderNo, from, to, req.customerId, req.materialId) }
            ?:run { headerRepo.findAllBySearchCondition(loginUser.compCd, req.orderNo, from, to, req.customerId) }
    }

    fun getDetailsByOrderNo(orderNo: String): OrderDetail? {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return detailRepo.findByOrderNoAndCompCdAndFlagActiveIsTrue(loginUser.compCd, orderNo)
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