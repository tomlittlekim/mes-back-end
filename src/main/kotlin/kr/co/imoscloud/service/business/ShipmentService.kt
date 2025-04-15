package kr.co.imoscloud.service.business

import kr.co.imoscloud.repository.business.ShipmentDetailRepository
import kr.co.imoscloud.repository.business.ShipmentHeaderRepository
import org.springframework.stereotype.Service

@Service
class ShipmentService(
    private val orderService: OrderService,
    private val headerRepo: ShipmentHeaderRepository,
    private val detailRepo: ShipmentDetailRepository
) {


}