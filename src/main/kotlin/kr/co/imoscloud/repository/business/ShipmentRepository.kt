package kr.co.imoscloud.repository.business

import kr.co.imoscloud.entity.business.ShipmentDetail
import kr.co.imoscloud.entity.business.ShipmentHeader
import kr.co.imoscloud.service.business.ShipmentDetailNullableDto
import kr.co.imoscloud.service.business.ShipmentHeaderNullableDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface ShipmentHeaderRepository: JpaRepository<ShipmentHeader, Long> {
    @Query("""
        select new kr.co.imoscloud.service.business.ShipmentHeaderNullableDto(
            sh.shipmentId as shipmentId,
            sh.site as site,
            sh.compCd as compCd,
            sh.orderNo as orderNo,
            oh.orderDate as orderDate,
            oh.ordererId as ordererId,
            oh.orderQuantity as orderQuantity,
            oh.customerId as customerId,
            oh.totalAmount as totalAmount,
            sh.shipmentStatus as shipmentStatus,
            sh.shippedQuantity as shippedQuantity,
            sh.unshippedQuantity as unshippedQuantity,
            sh.remark as remark
        )
        from ShipmentHeader sh
        left join OrderHeader oh on sh.orderNo = oh.orderNo
        where sh.compCd = :compCd
            and (:orderNo is null or sh.orderNo = :orderNo)
            and (:customerId is null or sh.customerId = :customerId)
            and (:shipmentStatus is null or sh.shipmentStatus = :shipmentStatus)
            and sh.createDate between :from and :to
            and sh.flagActive is true 
    """)
    fun getAllBySearchCondition(
        compCd: String?=null,
        orderNo: String?=null,
        customerId: String?=null,
        shipmentStatus: String?=null,
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<ShipmentHeaderNullableDto>
}

interface ShipmentDetailRepository: JpaRepository<ShipmentDetail, Long> {
}