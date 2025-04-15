package kr.co.imoscloud.service.business

import kr.co.imoscloud.repository.business.OrderDetailRepository
import kr.co.imoscloud.repository.business.OrderHeaderRepository
import org.springframework.stereotype.Service

@Service
class OrderService(
    val headerRepo: OrderHeaderRepository,
    val detailRepo: OrderDetailRepository
) {


}