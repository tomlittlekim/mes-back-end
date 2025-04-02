package kr.co.imoscloud.entity.user

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
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
    val compCd: String,

    @Column(name = "PRIORITY_LEVEL", length = 20)
    val priorityLevel: Int? = null,

    @Column(name = "ROLE_NAME", length = 100, nullable = false)
    val roleName: String,

    @Column(name = "FLAG_DEFAULT")
    val flagDefault: Boolean = false,

    @Column(name = "SEQUENCE")
    val sequence: Int? = null

): CommonCol(), DtoRoleIdBase