package kr.co.imoscloud.entity.business

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import kr.co.imoscloud.iface.DtoCompCdBase
import java.time.LocalDate

@Entity
@Table(name = "TRANSACTION_STATEMENT_DETAIL")
class TransactionStatementDetail(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    val id: Long = 0,

    @Column(name = "SITE", unique = true, nullable = false, length = 40)
    val site: String,

    @Column(name = "COMP_CD", unique = true, nullable = false, length = 40)
    override val compCd: String,

    @Column(name = "ORDER_NO", nullable = false, length = 100, unique = true)
    val orderNo: String,

    @Column(name = "ORDER_SUB_NO", nullable = false, length = 100, unique = true)
    val orderSubNo: String,

    @Column(name = "TRANSACTION_STATEMENT_ID")
    val flagIssuance: String? = null,

    @Column(name = "TRANSACTION_STATEMENT_DATE")
    val issuanceDate: LocalDate? = null,

): CommonCol(), DtoCompCdBase