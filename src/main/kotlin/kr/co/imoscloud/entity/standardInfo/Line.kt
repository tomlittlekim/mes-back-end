package kr.co.imoscloud.entity.standardInfo

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol

@Entity
@Table(
    name = "LINE",
    uniqueConstraints = [
        UniqueConstraint(
            name = "UK_LINE_SITE_COMP_FID_LID",
            columnNames = ["SITE", "COMP_CD", "FACTORY_ID", "LINE_ID"]
        )
    ]
)
class Line (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var id: Int? = null,

    @Column(name = "SITE", nullable = false, length = 20)
    var site: String? = null,

    @Column(name = "COMP_CD", nullable = false, length = 20)
    var compCd: String? = null,

    @Column(name = "FACTORY_ID", nullable = false, length = 100)
    var factoryId: String? = null,

    @Column(name = "LINE_ID", nullable = false, length = 100)
    var lineId: String? = null,

    @Column(name = "LINE_NAME", length = 100)
    var lineName: String? = null,

    @Column(name = "LINE_DESC", length = 20)
    var lineDesc: String? = null,
):CommonCol()