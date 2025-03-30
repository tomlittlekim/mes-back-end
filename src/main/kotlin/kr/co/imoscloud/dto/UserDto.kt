package kr.co.imoscloud.dto

import kr.co.imoscloud.iface.DtoBase
import kr.co.imoscloud.iface.ResponseVO.ResponseBase

data class UserInput(
    val id: Long? = null,
    var site: String?=null,
    var compCd: String?=null,
    var userId: String?=null,
    var password: String?=null,
    var userNm: String?=null,
    var email: String?=null,
    var roleId: String?=null,
    var phoneNum: String?=null,
    var departmentId: String?=null,
    var textarea: String?=null,
)

data class UserOutput(
    override val status: Int,
    override val message: String,
    override val id: Long,
    var userId: String?=null,
    var userNm: String?=null,
    var email: String?=null,
    var roleId: String?=null,
): ResponseBase, DtoBase

data class LoginRequest(val userId: String, val userPwd: String)

data class RoleSummery(
    val roleName: String,
    val priorityLevel: Int?,
)