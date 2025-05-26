package kr.co.imoscloud.entity.system

import jakarta.persistence.*
import kr.co.imoscloud.dto.RoleSummery
import kr.co.imoscloud.entity.CommonCol
import kr.co.imoscloud.iface.DtoCompCdBase
import kr.co.imoscloud.iface.DtoRoleIdBase

@Entity
@Table(name = "USER_ROLE")
class UserRole(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ROLE_ID")
    override val roleId: Long = 0L,

    @Column(name = "SITE", length = 40, nullable = false)
    val site: String,

    @Column(name = "COMP_CD", length = 40, nullable = false)
    override val compCd: String,

    @Column(name = "PRIORITY_LEVEL", nullable = false)
    var priorityLevel: Int = 0,

    @Column(name = "ROLE_NAME", length = 100, nullable = false)
    var roleName: String,

    @Column(name = "FLAG_DEFAULT")
    var flagDefault: Boolean = false,

    @Column(name = "SEQUENCE")
    var sequence: Int? = null

): CommonCol(), DtoRoleIdBase, DtoCompCdBase {

    companion object {
        fun toSummery(userRole: UserRole): RoleSummery = RoleSummery(
            userRole.roleId,
            userRole.compCd,
            userRole.roleName,
            userRole.priorityLevel
        )
    }
}