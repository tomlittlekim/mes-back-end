package kr.co.imoscloud.entity.standardInfo

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "LINE")
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

    @Column(name = "STATUS", length = 50)
    var status: String? = null,

    @Column(name = "LINE_DESC", length = 20)
    var lineDesc: String? = null,

    @Column(name = "FLAG_ACTIVE")
    var flagActive: Boolean? = null,

    @Column(name = "CREATE_USER", length = 100)
    var createUser: String? = null,

    @Column(name = "CREATE_DATE")
    var createDate: LocalDate? = null,

    @Column(name = "UPDATE_USER", length = 100)
    var updateUser: String? = null,

    @Column(name = "UPDATE_DATE")
    var updateDate: LocalDate? = null
)