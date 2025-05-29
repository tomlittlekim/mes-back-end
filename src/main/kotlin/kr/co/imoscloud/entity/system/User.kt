package kr.co.imoscloud.entity.system

import jakarta.persistence.*
import kr.co.imoscloud.dto.UserInput
import kr.co.imoscloud.dto.UserSummery
import kr.co.imoscloud.entity.CommonCol
import kr.co.imoscloud.iface.DtoAllInOneBase
import kr.co.imoscloud.security.UserPrincipal

@Entity
@Table(name = "USER")
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "SITE", length = 20)
    var site: String,

    @Column(name = "COMP_CD", length = 20)
    override var compCd: String,

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

) : CommonCol(), DtoAllInOneBase {

    companion object {
        fun create(req: UserInput, loginId: String, encodedPwd: String): User = User(
            site = req.site!!,
            compCd = req.compCd!!,
            loginId = loginId,
            userPwd = encodedPwd,
            userName = req.userName,
            userEmail = req.userEmail,
            roleId = req.roleId!!,
            phoneNum = req.phoneNum,
            departmentId = req.departmentId!!,
            positionId = req.positionId!!,
        )

        fun toSummery(u: User): UserSummery = UserSummery(
            u.id,
            u.site,
            u.compCd,
            u.userName,
            u.loginId,
            u.userPwd,
            u.imagePath,
            u.roleId,
            u.userEmail,
            u.phoneNum,
            u.departmentId,
            u.positionId,
            u.flagActive
        )

        fun createOwner(company: Company, loginId: String, encodedPwd: String): User = User(
            site = company.site,
            compCd = company.compCd,
            loginId = loginId,
            userPwd = encodedPwd,
            roleId = 2,
        )
    }

    fun modify(req: UserInput, encodedPwd: String?, loginUser: UserPrincipal): User = this.apply {
        site = req.site ?: this.site
        compCd = req.compCd ?: this.compCd
        loginId = req.loginId ?: this.loginId
        userPwd = encodedPwd ?: this.userPwd
        userName = req.userName ?: this.userName
        userEmail = req.userEmail ?: this.userEmail
        imagePath = req.imagePath ?: this.imagePath
        roleId = req.roleId ?: this.roleId
        phoneNum = req.phoneNum ?: this.phoneNum
        departmentId = req.departmentId ?: this.departmentId
        positionId = req.positionId ?: this.positionId
        flagActive = true
        updateCommonCol(loginUser)
    }
}