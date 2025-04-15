package kr.co.imoscloud.fetcher

import com.netflix.graphql.dgs.DgsComponent
import kr.co.imoscloud.service.business.OrderService
import kr.co.imoscloud.service.business.ShipmentService

@DgsComponent
class BusinessFetcher(
    private val oderService: OrderService,
    private val shipmentService: ShipmentService
) {


}