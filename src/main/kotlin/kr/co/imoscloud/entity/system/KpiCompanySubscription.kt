package kr.co.imoscloud.entity.system

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol

@Entity
@Table(name = "KPI_COMPANY_SUBSCRIPTION")
class KpiCompanySubscription(
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

    @Column(name = "CATEGORY_ID", nullable = false)
    var categoryId: String,

    @Column(name = "DESCRIPTION")
    var description: String? = null,

    @Column(name = "SORT")
    var sort: Int? = null
): CommonCol()