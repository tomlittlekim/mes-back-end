package kr.co.imoscloud.entity.kpi

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol

@Entity
@Table(
    name = "KPI_INDICATOR_MASTER",
    uniqueConstraints = [
        UniqueConstraint(
            name = "UK_KPI_INDICATOR_MASTER",
            columnNames = ["SITE", "COMP_CD", "KPI_INDICATOR_CD"]
        )
    ]
)
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

    @Column(name = "UNIT")
    var unit: String? = null,

    @Column(name = "UPDATE_CYCLE")
    var updateCycle: String? = null,

    @Column(name = "CHART_TYPE")
    var chartType: String? = null
) : CommonCol()