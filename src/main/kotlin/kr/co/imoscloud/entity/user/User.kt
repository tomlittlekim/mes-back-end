package kr.co.imoscloud.entity.user

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol

@Entity
@Table(name = "USER")
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "SITE", length = 20)
    val site: String,

    @Column(name = "COMP_CD", length = 20)
    val compCd: String,

    @Column(name = "EMPLOYEE_NUM", length = 20)
    val employeeNum: String,

    @Column(name = "USER_NAME", length = 20)
    val userName: String,

    @Column(name = "LOGIN_ID", length = 100, unique = true)
    val loginId: String,

    @Column(name = "USER_PWD", length = 100)
    val userPwd: String,

    @Column(name = "IMAGE_PATH", length = 50)
    val imagePath: String? = null,

    @Column(name = "ROLE_ID")
    val roleId: Long,

    @Column(name = "USER_EMAIL", length = 20)
    val userEmail: String,

    @Column(name = "PHONE_NUM", length = 11)
    val phoneNum: String,

    @Column(name = "DEPARTMENT_ID", length = 20)
    val departmentId: String,

    @Column(name = "TEXT_AREA", length = 1000)
    val textArea: String? = null,

    @Column(name = "FLAG_LOCK")
    val flagLock: Boolean = false

) : CommonCol()