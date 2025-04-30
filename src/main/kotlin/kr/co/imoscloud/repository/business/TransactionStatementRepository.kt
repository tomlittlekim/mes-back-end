package kr.co.imoscloud.repository.business

import kr.co.imoscloud.entity.business.TransactionStatementDetail
import kr.co.imoscloud.entity.business.TransactionStatementHeader
import kr.co.imoscloud.service.business.TransactionStatementHeaderNullableDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface TransactionStatementHeaderRepository: JpaRepository<TransactionStatementHeader, Long> {

    @Query(
        """
        select new kr.co.imoscloud.service.business.TransactionStatementHeaderNullableDto(
            ts.id,
            ts.site,
            ts.compCd,
            ts.orderNo,
            oh.orderDate,
            v.vendorName,
            oh.orderQuantity,
            oh.finalAmount,
            oh.totalAmount,
            oh.vatAmount,
            ts.flagIssuance,
            ts.issuanceDate,
            oh.flagVatAmount
        )
        from TransactionStatementHeader ts
        left join OrderHeader oh on ts.orderNo = oh.orderNo
            and ts.compCd = oh.compCd
            and oh.flagActive is true
        left join ShipmentHeader sh on ts.orderNo = sh.orderNo
            and ts.compCd = sh.compCd
            and oh.flagActive is true
        left join Vendor v on oh.customerId = v.vendorId
            and v.compCd = ts.compCd
            and v.flagActive is true
        where ts.site = :site
            and ts.compCd = :compCd
            and (:id is null or ts.id = :id)
            and ts.createDate between :from and :to
            and (:orderNo is null or ts.orderNo = :orderNo)
            and (:customerId is null or oh.customerId = :customerId)
            and ts.flagActive is true
    """
    )
    fun getAllBySearchCondition(
        site: String,
        compCd: String,
        id: Long?=null,
        from: LocalDateTime?=null,
        to: LocalDateTime?=null,
        orderNo: String?=null,
        customerId: String?=null,
    ): List<TransactionStatementHeaderNullableDto>
}

interface TransactionStatementDetailRepository: JpaRepository<TransactionStatementDetail, Long> {
}