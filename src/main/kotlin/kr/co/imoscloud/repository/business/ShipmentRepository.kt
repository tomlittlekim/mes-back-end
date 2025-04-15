package kr.co.imoscloud.repository.business

import kr.co.imoscloud.entity.business.ShipmentDetail
import kr.co.imoscloud.entity.business.ShipmentHeader
import org.springframework.data.jpa.repository.JpaRepository

interface ShipmentHeaderRepository: JpaRepository<ShipmentHeader, Long> {
}

interface ShipmentDetailRepository: JpaRepository<ShipmentDetail, Long> {
}