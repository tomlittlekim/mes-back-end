package kr.co.imoscloud.repository.business

import kr.co.imoscloud.entity.business.OrderDetail
import kr.co.imoscloud.entity.business.OrderHeader
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface OrderHeaderRepository: JpaRepository<OrderHeader, Long> {
    @Query("""
        select oh
        from OrderHeader oh
        where oh.compCd = :compcd
            and (:orderNo is null or oh.orderNo = :orderNo)
            and (:fromDate is null or (oh.createDate between :fromDate and :toDate))
            and (:customerId is null or oh.customerId = :customerId)
            and oh.flagActive is true 
    """)
    fun findAllBySearchCondition(
        compCd: String,
        orderNo: String?=null,
        fromDate: LocalDateTime?,
        toDate: LocalDateTime?,
        customerId: String?=null,
    ): List<OrderHeader>

    @Query("""
        select oh.orderNo
        from OrderHeader oh
        where oh.compCd = :compcd
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
            FINAL_AMOUNT = 
                CASE 
                    WHEN FLAG_VAT_AMOUNT = true THEN TOTAL_AMOUNT + VAT_AMOUNT
                    ELSE TOTAL_AMOUNT
                END
        WHERE ORDER_NO = :orderNo
          AND FLAG_ACTIVE = true
    """, nativeQuery = true)
    fun updateAmountsByDetailPrice(orderNo: String, totalAmount: Int, vatAmount: Int): Int

    @Modifying
    @Query("""
        update OrderHeader oh
        set 
            oh.flagActive = false,
            oh.updateDate = now(),
            oh.updateUser = : updateUserId
        where oh.site = :site
            and oh.compCd = :compCd
            and oh.id = :id
            and oh.flagActive is true
    """)
    fun deleteOrderHeader(site:String, compCd:String, id: Long, updateUserId: String): Int
}

interface OrderDetailRepository: JpaRepository<OrderDetail, Long> {
    @Query("""
        select oh
        from OrderDetail od
        left join OrderHeader oh on od.orderNo = oh.orderNo
        where od.compCd = :compcd
            and (:orderNo is null or od.orderNo = :orderNo)
            and (:fromDate is null or (od.createDate between :fromDate and :toDate))
            and (:customerId is null or oh.customerId = :customerId)
            and (:materialId is null or od.systemMaterialId = :materialId)
            and od.flagActive is true 
    """)
    fun findAllBySearchCondition(
        compCd: String,
        orderNo: String?=null,
        fromDate: LocalDateTime?,
        toDate: LocalDateTime?,
        customerId: String?=null,
        materialId: String?=null,
    ): List<OrderHeader>

    fun findAllByOrderNoAndCompCdAndFlagActiveIsTrue(compCd: String, orderNo: String?): List<OrderDetail>

    @Query("""
        select od.orderSubNo
        from OrderDetail od
        where od.compCd = :compcd
        order by od.createDate desc limit 1
    """)
    fun getLatestOrderSubNo(compCd: String): String?

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
            AND oh.COMP_CD = :compcd
            AND oh.ID = :id
            AND od.FLAG_ACTIVE is true
    """, nativeQuery = true)
    fun deleteAllByOrderHeaderId(site: String, compCd: String, id: Long, updateUserId: String): Int
}