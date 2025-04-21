package kr.co.imoscloud.repository.business

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.business.ShipmentDetail
import kr.co.imoscloud.entity.business.ShipmentHeader
import kr.co.imoscloud.service.business.ShipmentDetailNullableDto
import kr.co.imoscloud.service.business.ShipmentHeaderNullableDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
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
    @Query("""
        select new kr.co.imoscloud.service.business.ShipmentDetailNullableDto(
            sd.id as id,
            sd.site as site,
            sd.compCd as compCd,
            sd.orderNo as orderNo,
            sd.orderSubNo as orderSubNo,
            od.systemMaterialId as systemMaterialId,
            mm.materialName as materialName,
            mm.materialStandard as materialStandard,
            mm.unit as unit,
            od.quantity as quantity,
            100 as stockQuantity,
            sd.shipmentId as shipmentId,
            sd.shipmentDate as shipmentDate,
            sd. shippedQuantity as shippedQuantity,
            sd.unshippedQuantity as unshippedQuantity,
            sd.cumulativeShipmentQuantity as cumulativeShipmentQuantity,
            sd.shipmentWarehouse as shipmentWarehouse,
            sd.shipmentHandler as shipmentHandler,
            sd.remark as remark
        )
        from ShipmentDetail sd
        left join OrderDetail od on sd.orderNo = od.orderNo 
            and sd.orderSubNo = od.orderSubNo
            and od.flagActive is true
        left join MaterialMaster mm on od.systemMaterialId = mm.systemMaterialId
            and mm.flagActive is true
        where sd.compCd = :compCd
            and sd.shipmentId = :shipmentId
            and sd.flagActive is true
    """)
    fun findAllByCompCdAndShipmentIdAndFlagActiveIsTrue(compCd: String, shipmentId: Long): List<ShipmentDetailNullableDto>

    @Transactional
    @Modifying
    @Query("""
        UPDATE SHIPMENT_DETAIL sd
        JOIN SHIPMENT_HEADER sh ON sh.SHIPMENT_ID = sd.SHIPMENT_ID
        SET 
            sd.FLAG_ACTIVE = FALSE,
            sd.UPDATE_USER = :loginUser,
            sd.UPDATE_DATE = :updateDate,
            sh.SHIPPED_QUANTITY = sh.SHIPPED_QUANTITY - sd.CUMULATIVE_SHIPMENT_QUANTITY,
            sh.UNSHIPPED_QUANTITY = sh.UNSHIPPED_QUANTITY + sd.CUMULATIVE_SHIPMENT_QUANTITY
        WHERE sd.SITE = :site
          AND sd.COMP_CD = :compCd
          AND sd.SHIPMENT_ID = :shipmentId
          AND sd.FLAG_ACTIVE = TRUE
    """, nativeQuery = true)
    fun softDelete(
        site: String,
        compCd: String,
        shipmentId: Long,
        loginUser: String,
        updateDate: LocalDateTime?= LocalDateTime.now()
    ): Int
}