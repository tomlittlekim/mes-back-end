package com.example.imosbackend.entity

import com.example.imosbackend.dto.UserRequest
import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

@Entity
@Table(name = "users")
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "SITE", length = 20)
    val site: String,

    @Column(name = "COMP_CD", length = 20)
    val compCd: String,

    @Column(name = "USER_NAME", length = 20)
    val userName: String? = null,

    @Column(name = "USER_ID", length = 100, unique = true)
    val userId: String,

    @Column(name = "USER_PWD", length = 100)
    val userPwd: String,

    @Column(name = "IMAGE_PATH", length = 50)
    val imagePath: String? = null,

    @Column(name = "ROLE_ID", length = 20)
    val roleId: String? = null,

    @Column(name = "USER_EMAIL", length = 20)
    val userEmail: String? = null,

    @Column(name = "PHONE_NUM", length = 11)
    val phoneNum: String? = null,

    @Column(name = "DEPARTMENT_ID", length = 20)
    val departmentId: String? = null,

    @Column(name = "TEXT_AREA", length = 1000)
    val textArea: String? = null,

    @Column(name = "IS_LOCK")
    val isLock: Boolean = false

) : CommonCol()