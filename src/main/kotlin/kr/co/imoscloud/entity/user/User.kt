package kr.co.imoscloud.entity.user

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import kr.co.imoscloud.iface.DtoAllInOneBase

@Entity
@Table(name = "USER")
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "SITE", length = 20)
    val site: String,

    @Column(name = "COMP_CD", length = 20)
    override val compCd: String,

    @Column(name = "USER_NAME", length = 20)
    var userName: String? = null,

    @Column(name = "LOGIN_ID", length = 100, unique = true)
    override var loginId: String,

    @Column(name = "USER_PWD", length = 100, nullable = false)
    var userPwd: String,

    @Column(name = "IMAGE_PATH", length = 100)
    var imagePath: String? = null,

    @Column(name = "ROLE_ID")
    override var roleId: Long,

    @Column(name = "USER_EMAIL", length = 100)
    var userEmail: String? = null,

    @Column(name = "PHONE_NUM", length = 11)
    var phoneNum: String? = null,

    @Column(name = "DEPARTMENT_ID", length = 100)
    var departmentId: String? = null,

    @Column(name = "POSITION_ID", length = 100)
    var positionId: String? = null,

    @Column(name = "FLAG_LOCK")
    var flagLock: Boolean = false

) : CommonCol(), DtoAllInOneBase