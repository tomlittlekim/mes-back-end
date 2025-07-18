package kr.co.imoscloud.entity.material

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol

@Entity
@Table(
    name = "BOM",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uc_mos_bom_site_comp_bom",
            columnNames = ["SITE", "COMP_CD", "BOM_ID"]
        )
    ]
)
class Bom(
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

    @Column(name = "BOM_LEVEL", nullable = false)
    var bomLevel: Int = 1,

    @Column(name = "ITEM_CD", nullable = false, length = 50)
    var itemCd: String? = null,

    @Column(name = "BOM_NAME", length = 100)
    var bomName: String? = null,

    @Column(name = "REMARK", length = 500)
    var remark: String? = null,
): CommonCol()