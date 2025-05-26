package kr.co.imoscloud.entity.system

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import kr.co.imoscloud.security.UserPrincipal
import java.time.LocalDateTime

@Entity
@Table(name = "KPI_INDICATOR_MASTER")
class KpiIndicatorMaster(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ")
    var seq: Long? = null,

    @Column(name = "SITE", nullable = false)
    var site: String,

    @Column(name = "COMP_CD", nullable = false)
    var compCd: String,

    @Column(name = "KPI_INDICATOR_CD", nullable = false)
    var kpiIndicatorCd: String,

    @Column(name = "KPI_INDICATOR_NM")
    var kpiIndicatorNm: String? = null,

    @Column(name = "CATEGORY_CD", nullable = false)
    var categoryCd: String,

    @Column(name = "DESCRIPTION")
    var description: String? = null,

    @Column(name = "TARGET_VALUE")
    var targetValue: Double? = null,

    @Column(name = "UNIT")
    var unit: String? = null,

    @Column(name = "UPDATE_CYCLE")
    var updateCycle: String? = null,

    @Column(name = "CHART_TYPE")
    var chartType: String? = null
) : CommonCol()