package kr.co.imoscloud.entity.kpi

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol

@Entity
@Table(name = "KPI_CATEGORY_MASTER")
class KpiCategoryMaster(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ")
    var seq: Long? = null,

    @Column(name = "SITE", nullable = false)
    var site: String,

    @Column(name = "COMP_CD", nullable = false)
    var compCd: String,

    @Column(name = "CATEGORY_CD", nullable = false)
    var categoryCd: String,

    @Column(name = "CATEGORY_NM")
    var categoryNm: String? = null,

    @Column(name = "DESCRIPTION")
    var description: String? = null
) : CommonCol()