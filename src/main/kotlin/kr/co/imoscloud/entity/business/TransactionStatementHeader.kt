package kr.co.imoscloud.entity.business

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import kr.co.imoscloud.iface.DtoCompCdBase
import java.time.LocalDate

@Entity
@Table(name = "TRANSACTION_STATEMENT_HEADER")
class TransactionStatementHeader(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    val id: Long = 0,

    @Column(name = "SITE", unique = true, nullable = false, length = 20)
    val site: String,

    @Column(name = "COMP_CD", unique = true, nullable = false, length = 20)
    override val compCd: String,

    @Column(name = "ORDER_NO", nullable = false, length = 100, unique = true)
    val orderNo: String,

    @Column(name = "FLAG_ISSUANCE")
    var flagIssuance: Boolean? = false,

    @Column(name = "ISSUANCE_DATE")
    var issuanceDate: LocalDate? = null,

): CommonCol(), DtoCompCdBase