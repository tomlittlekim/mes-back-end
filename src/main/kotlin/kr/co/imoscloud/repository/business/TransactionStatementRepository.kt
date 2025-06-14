package kr.co.imoscloud.repository.business

import kr.co.imoscloud.entity.business.TransactionStatementDetail
import kr.co.imoscloud.entity.business.TransactionStatementHeader
import kr.co.imoscloud.service.business.ShipmentDetailWithMaterialDto
import kr.co.imoscloud.service.business.TransactionStatementDetailNullableDto
import kr.co.imoscloud.service.business.TransactionStatementHeaderNullableDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
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
            and oh.orderDate between :from and :to
            and (:orderNo is null or ts.orderNo = :orderNo)
            and (:customerId is null or oh.customerId = :customerId)
            and ts.flagActive is true
    """
    )
    fun getAllBySearchCondition(
        site: String,
        compCd: String,
        id: Long?=null,
        from: LocalDate,
        to: LocalDate,
        orderNo: String?=null,
        customerId: String?=null,
    ): List<TransactionStatementHeaderNullableDto>

    fun findByIdAndFlagActiveIsTrue(id: Long): TransactionStatementHeader?
    fun findBySiteAndCompCdAndOrderNoAndFlagActiveIsTrue(site: String, compCd: String, orderNo: String): TransactionStatementHeader?
}

interface TransactionStatementDetailRepository: JpaRepository<TransactionStatementDetail, Long> {

    @Query("""
        select new kr.co.imoscloud.service.business.TransactionStatementDetailNullableDto(
            tsd.id,
            tsd.site,
            tsd.compCd,
            tsd.orderNo,
            tsd.orderSubNo,
            tsd.transactionStatementId,
            tsd.transactionStatementDate,
            sh.systemMaterialId,
            mm.materialName,
            mm.materialStandard,
            mm.unit,
            sh.cumulativeShipmentQuantity,
            od.unitPrice,
            null,
            null
        )
        from TransactionStatementDetail tsd
        left join OrderDetail od on tsd.compCd = od.compCd
            and tsd.orderNo = od.orderNo
            and tsd.orderSubNo = od.orderSubNo
            and od.flagActive is true
        left join ShipmentDetail sh on tsd.compCd = sh.compCd
            and tsd.orderNo = sh.orderNo
            and tsd.orderSubNo = sh.orderSubNo
            and tsd.shipmentDetailId = sh.id
            and sh.flagActive is true
        left join MaterialMaster mm on tsd.compCd = mm.compCd
            and sh.systemMaterialId = mm.systemMaterialId
            and mm.flagActive is true
        where tsd.site = :site
            and tsd.compCd = :compCd
            and tsd.orderNo = :orderNo
            and tsd.flagActive is true
    """)
    fun getAllInitialByOrderNo(
        site: String,
        compCd: String,
        orderNo: String,
    ): List<TransactionStatementDetailNullableDto>

    @Query(
        value = """
        SELECT 
            sd.ORDER_NO AS orderNo,
            sd.ORDER_SUB_NO AS orderSubNo,
            sd.SYSTEM_MATERIAL_ID AS systemMaterialId,
            mm.MATERIAL_NAME AS materialName,
            mm.MATERIAL_STANDARD AS materialStandard,
            mm.UNIT AS unit,
            CAST((sd.SHIPPED_QUANTITY + sd.CUMULATIVE_SHIPMENT_QUANTITY) AS DOUBLE) AS quantity
        FROM SHIPMENT_DETAIL sd
        LEFT JOIN MATERIAL_MASTER mm
            ON sd.SYSTEM_MATERIAL_ID = mm.SYSTEM_MATERIAL_ID
            AND sd.COMP_CD = mm.COMP_CD
            AND mm.FLAG_ACTIVE = true
        WHERE sd.SITE = :site
            AND sd.COMP_CD = :compCd
            AND sd.ORDER_NO = :orderNo
            AND sd.ORDER_SUB_NO = :orderSubNo
            AND sd.FLAG_ACTIVE = true
        ORDER BY sd.CREATE_DATE DESC
        LIMIT 1
    """,
        nativeQuery = true
    )
    fun getAllLatestByOrderNo(
        site: String,
        compCd: String,
        orderNo: String,
        orderSubNo: String
    ): ShipmentDetailWithMaterialDto?

    fun findAllBySiteAndCompCdAndOrderNoAndFlagActiveIsTrue(
        site: String,
        compCd: String,
        orderNo: String
    ): List<TransactionStatementDetail>

    @Modifying
    @Query("""
        update TransactionStatementDetail tsd
        set
            tsd.transactionStatementId = :tsId,
            tsd.transactionStatementDate = :tsDate,
            tsd.updateDate = :localDateTime,
            tsd.updateUser = :user
        where tsd.compCd = :compCd
            and tsd.id in :ids
            and tsd.flagActive is true 
    """)
    fun updateTSByIdIn(
        tsId: String,
        tsDate: LocalDate,
        user: String,
        compCd: String,
        ids: List<Long>,
        localDateTime: LocalDateTime?=LocalDateTime.now()
    ): Int
}