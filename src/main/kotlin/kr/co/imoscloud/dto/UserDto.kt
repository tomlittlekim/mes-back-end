package kr.co.imoscloud.dto

import kr.co.imoscloud.iface.*
import kr.co.imoscloud.iface.ResponseVO.ResponseBase

data class UserInput(
    val id: Long? = null,
    var site: String?=null,
    var compCd: String?=null,
    var userId: String?=null,
    var password: String?=null,
    var userNm: String?=null,
    var email: String?=null,
    var roleId: Long?=null,
    var phoneNum: String?=null,
    var departmentId: String?=null,
    var textarea: String?=null,
)

data class LoginOutput(
    override val status: Int,
    override val message: String,
    val id: Long,
    val loginId: String?=null,
    var userNm: String?=null,
    var email: String?=null,
    override var roleId: Long,
    var roleNm: String?=null,
): ResponseBase, DtoRoleIdBase

data class LoginRequest(val userId: String, val userPwd: String)

data class UserSummery(
    override val loginId: String,
    override val roleId: Long,
    val userPwd: String,
    val userName: String?=null,
    val email: String?=null
): DtoLoginIdBase, DtoRoleIdBase

data class ExistLoginIdRequest(
    override val loginId: String
): DtoLoginIdBase

data class RoleInput(
    override val roleId: Long,
): DtoRoleIdBase

data class RoleSummery(
    val roleName: String,
    val priorityLevel: Int?,
)

data class TestAllInOneDto(
    override val loginId: String,
    override val roleId: Long,
    override val compCd: String
): DtoAllInOneBase