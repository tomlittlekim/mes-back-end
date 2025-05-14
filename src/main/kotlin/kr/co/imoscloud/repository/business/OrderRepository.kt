package kr.co.imoscloud.repository.business

import kr.co.imoscloud.entity.business.OrderDetail
import kr.co.imoscloud.entity.business.OrderHeader
import kr.co.imoscloud.service.business.ShipmentDetailNullableDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.time.LocalDateTime

interface OrderHeaderRepository: JpaRepository<OrderHeader, Long> {
    @Query("""
        select oh
        from OrderHeader oh
        where oh.compCd = :compCd
            and (:orderNo is null or oh.orderNo = :orderNo)
            and (:fromDate is null or (oh.orderDate between :fromDate and :toDate))
            and (:customerId is null or oh.customerId = :customerId)
            and oh.flagActive is true 
    """)
    fun findAllBySearchCondition(
        compCd: String,
        orderNo: String?=null,
        fromDate: LocalDate,
        toDate: LocalDate,
        customerId: String?=null,
    ): List<OrderHeader>

    @Query("""
        select oh.orderNo
        from OrderHeader oh
        where oh.compCd = :compCd
        order by oh.createDate desc limit 1
    """)
    fun getLatestOrderNo(compCd: String): String?

    fun findBySiteAndCompCdAndIdAndFlagActiveIsTrue(site:String, compCd: String, id: Long): OrderHeader?
    fun findAllBySiteAndCompCdAndIdInAndFlagActiveIsTrue(site:String, compCd: String, id: List<Long>): List<OrderHeader>

    @Modifying
    @Query("""
        UPDATE ORDER_HEADER
        SET 
            TOTAL_AMOUNT = TOTAL_AMOUNT + :totalAmount,
            VAT_AMOUNT = VAT_AMOUNT + :vatAmount,
            ORDER_QUANTITY = ORDER_QUANTITY + :orderQuantity,
            FINAL_AMOUNT = 
                CASE 
                    WHEN FLAG_VAT_AMOUNT = true THEN TOTAL_AMOUNT + VAT_AMOUNT
                    ELSE TOTAL_AMOUNT
                END
        WHERE ORDER_NO = :orderNo
          AND FLAG_ACTIVE = true
    """, nativeQuery = true)
    fun updateAmountsByDetailPrice(orderNo: String, totalAmount: Int, vatAmount: Int, orderQuantity: Double): Int

    @Modifying
    @Query("""
    update OrderHeader oh
    set 
        oh.flagActive = false,
        oh.updateDate = :updateDate,
        oh.updateUser = :updateUserId
    where oh.site = :site
      and oh.compCd = :compCd
      and oh.id = :id
      and oh.flagActive = true
""")
    fun deleteOrderHeader(
        @Param("site") site: String,
        @Param("compCd") compCd: String,
        @Param("id") id: Long,
        @Param("updateUserId") updateUserId: String,
        @Param("updateDate") updateDate: LocalDateTime = LocalDateTime.now() // 기본값도 설정 가능
    ): Int
}

interface OrderDetailRepository: JpaRepository<OrderDetail, Long> {
    @Query("""
        select oh
        from OrderDetail od
        left join OrderHeader oh on od.orderNo = oh.orderNo
        where od.compCd = :compCd
            and (:orderNo is null or od.orderNo = :orderNo)
            and (:fromDate is null or (od.createDate between :fromDate and :toDate))
            and (:customerId is null or oh.customerId = :customerId)
            and (:materialId is null or od.systemMaterialId = :materialId)
            and od.flagActive is true 
    """)
    fun findAllBySearchCondition(
        compCd: String,
        orderNo: String?=null,
        fromDate: LocalDate,
        toDate: LocalDate,
        customerId: String?=null,
        materialId: String?=null,
    ): List<OrderHeader>

    fun findAllByCompCdAndOrderNoAndFlagActiveIsTrue(compCd: String, orderNo: String): List<OrderDetail>

    @Query("""
        select od.orderSubNo
        from OrderDetail od
        where od.compCd = :compCd
            and od.orderNo = :orderNo
        order by od.createDate desc limit 1
    """)
    fun getLatestOrderSubNo(compCd: String, orderNo: String): String?

    fun findBySiteAndCompCdAndIdAndFlagActiveIsTrue(site: String, compCd: String, id: Long): OrderDetail?
    fun findAllBySiteAndCompCdAndIdInAndFlagActiveIsTrue(site: String, compCd: String, id: List<Long>): List<OrderDetail>

    @Modifying
    @Query("""
        UPDATE ORDER_HEADER oh
        JOIN ORDER_DETAIL od ON oh.ORDER_NO = od.ORDER_NO
        SET 
            od.FLAG_ACTIVE = FALSE,
            od.UPDATE_DATE = NOW(),
            od.UPDATE_USER = :updateUserId
        WHERE oh.SITE = :site
            AND oh.COMP_CD = :compCd
            AND oh.ID = :id
            AND od.FLAG_ACTIVE is true
    """, nativeQuery = true)
    fun deleteAllByOrderHeaderId(site: String, compCd: String, id: Long, updateUserId: String): Int

    @Query("""
        select new kr.co.imoscloud.service.business.ShipmentDetailNullableDto(
            null,
            od.site,
            od.compCd,
            od.orderNo,
            od.orderSubNo,
            od.systemMaterialId,
            mm.materialName,
            mm.materialStandard,
            mm.unit,
            od.quantity,
            its.qty,
            null,
            null,
            null,
            null,
            null,
            its.warehouseId,
            null,
            null
        )
        from OrderDetail od
        left join MaterialMaster mm on od.systemMaterialId = mm.systemMaterialId
            and od.compCd = mm.compCd
            and mm.flagActive is true
        left join InventoryStatus its on od.systemMaterialId = its.systemMaterialId
            and od.compCd = its.compCd
            and its.warehouseId = :warehouseId
            and its.flagActive is true
        where od.site = :site
            and od.compCd = :compCd
            and od.orderNo = :orderNo
            and od.orderSubNo = :orderSubNo
            and od.flagActive is true
    """)
    fun getInitialByRequest(
        site: String,
        compCd: String,
        orderNo: String,
        orderSubNo: String,
        warehouseId: String
    ): ShipmentDetailNullableDto?
}