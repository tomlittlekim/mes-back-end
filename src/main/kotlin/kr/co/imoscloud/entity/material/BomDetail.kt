package kr.co.imoscloud.entity.material

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import kr.co.imoscloud.security.UserPrincipal
import java.time.LocalDateTime

@Entity
@Table(name = "BOM_DTL",)
class BomDetail : CommonCol() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var seq: Int? = null

    @Column(name = "SITE", nullable = false, length = 50)
    var site: String? = null

    @Column(name = "COMP_CD", nullable = false, length = 50)
    var compCd: String? = null

    @Column(name = "BOM_ID", nullable = false, length = 50)
    var bomId: String? = null

    @Column(name = "BOM_DETAIL_ID", nullable = false, length = 50)
    var bomDetailId: String? = null

    @Column(name = "BOM_LEVEL", nullable = false)
    var bomLevel: Int? = null

    @Column(name = "ITEM_CD", nullable = false, length = 50)
    var itemCd: String? = null

    @Column(name = "PARENT_ITEM_CD", length = 50)
    var parentItemCd: String? = null

    @Column(name = "ITEM_QTY")
    var itemQty: Double? = null

    @Column(name = "REMARK", length = 500)
    var remark: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(
        JoinColumn(name = "SITE", referencedColumnName = "SITE", insertable = false, updatable = false),
        JoinColumn(name = "COMP_CD", referencedColumnName = "COMP_CD", insertable = false, updatable = false),
        JoinColumn(name = "BOM_ID", referencedColumnName = "BOM_ID", insertable = false, updatable = false)
    )
    var bom: Bom? = null
} 