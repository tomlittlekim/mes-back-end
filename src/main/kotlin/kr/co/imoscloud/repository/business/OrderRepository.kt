package kr.co.imoscloud.repository.business

import kr.co.imoscloud.entity.business.OrderDetail
import kr.co.imoscloud.entity.business.OrderHeader
import org.springframework.data.jpa.repository.JpaRepository

interface OrderHeaderRepository: JpaRepository<OrderHeader, Long> {
}

interface OrderDetailRepository: JpaRepository<OrderDetail, Long> {
}