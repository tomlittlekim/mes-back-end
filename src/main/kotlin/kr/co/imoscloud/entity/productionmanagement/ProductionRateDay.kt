package kr.co.imoscloud.entity.productionmanagement

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDateTime

@Entity
@Table(name = "PRODUCTION_RATE_DAY")
class ProductionRateDay (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var id: Long? = null,

    @Column(name = "SITE", nullable = false, length = 20)
    var site: String? = null,

    @Column(name = "COMP_CD", nullable = false, length = 20)
    var compCd: String? = null,

    @Column(name = "PLAN_SUM")
    var planSum: Double? = null,

    @Column(name = "WORK_ORDER_SUM")
    var workOrderSum: Double? = null,

    @Column(name = "NOT_WORK_ORDER_SUM")
    var notWorkOrderSum: Double? = null,

    @Column(name = "PRODUCTION_RATE")
    var productionRate: Double? = null,

    @Column(name = "AGGREGATION_TIME", nullable = false)
    var aggregationTime: LocalDateTime? = null,

    @Column(name = "CREATE_USER", length = 100)
    var createUser: String? = null,

    @Column(name = "CREATE_DATE", nullable = false)
    var createDate: LocalDateTime? = null,

    @Column(name = "UPDATE_USER", length = 100)
    var updateUser: String? = null,

    @Column(name = "UPDATE_DATE", nullable = false)
    var updateDate: LocalDateTime? = null
)