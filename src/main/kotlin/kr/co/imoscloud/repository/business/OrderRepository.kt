package kr.co.imoscloud.repository.business

import kr.co.imoscloud.entity.business.OrderDetail
import kr.co.imoscloud.entity.business.OrderHeader
import org.springframework.data.jpa.repository.JpaRepository
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
}