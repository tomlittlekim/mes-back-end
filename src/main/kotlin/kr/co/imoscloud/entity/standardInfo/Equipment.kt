package kr.co.imoscloud.entity.standardInfo

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "EQUIPMENT")
class Equipment (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var id: Int? = null,

    @Column(name = "SITE", nullable = false, length = 20)
    var site: String? = null,

    @Column(name = "COMP_CD", nullable = false, length = 20)
    var compCd: String? = null,

    @Column(name = "EQUIPMENT_ID", nullable = false, length = 50)
    var equipmentId: String? = null,

    @Column(name = "FACTORY_ID", length = 50)
    var factoryId: String? = null,

    @Column(name = "LINE_ID", length = 50)
    var lineId: String? = null,

    @Column(name = "EQUIPMENT_SN", length = 50)
    var equipmentSn: String? = null,

    @Column(name = "EQUIPMENT_TYPE", length = 20)
    var equipmentType: String? = null,

    @Column(name = "EQUIPMENT_NAME", length = 20)
    var equipmentName: String? = null,

    @Column(name = "EQUIPMENT_STATUS", length = 20)
    var equipmentStatus: String? = null,

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