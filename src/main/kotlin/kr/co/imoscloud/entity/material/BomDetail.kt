package kr.co.imoscloud.entity.material

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol

@Entity
@Table(
    name = "BOM_DETAIL",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uc_mos_bom_dtl_site_comp_bom_detail",
            columnNames = ["SITE", "COMP_CD", "BOM_ID", "BOM_DETAIL_ID"]
        )
    ]
)
class BomDetail(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var seq: Int? = null,

    @Column(name = "SITE", nullable = false, length = 50)
    var site: String? = null,

    @Column(name = "COMP_CD", nullable = false, length = 50)
    var compCd: String? = null,

    @Column(name = "BOM_ID", nullable = false, length = 50)
    var bomId: String? = null,

    @Column(name = "BOM_DETAIL_ID", nullable = false, length = 50)
    var bomDetailId: String? = null,

    @Column(name = "BOM_LEVEL", nullable = false)
    var bomLevel: Int? = null,

    @Column(name = "ITEM_CD", nullable = false, length = 50)
    var itemCd: String? = null,

    @Column(name = "PARENT_ITEM_CD", length = 50)
    var parentItemCd: String? = null,

    @Column(name = "ITEM_QTY")
    var itemQty: Double? = null,

    @Column(name = "REMARK", length = 500)
    var remark: String? = null
) : CommonCol()