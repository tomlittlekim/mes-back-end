package kr.co.imoscloud.dto

import kr.co.imoscloud.iface.DtoAllInOneBase
import kr.co.imoscloud.iface.DtoRoleIdBase
import kr.co.imoscloud.iface.DtoUserIdBase
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

data class UserOutput(
    override val status: Int,
    override val message: String,
    override val userId: Long,
    var loginId: String?=null,
    var userNm: String?=null,
    var email: String?=null,
    override var roleId: Long,
    var roleNm: String?=null,
): ResponseBase, DtoUserIdBase, DtoRoleIdBase

data class LoginRequest(val userId: String, val userPwd: String)

data class RoleInput(
    override val roleId: Long,
): DtoRoleIdBase

data class RoleSummery(
    val roleName: String,
    val priorityLevel: Int?,
)

data class TestAllInOneDto(
    override val userId: Long,
    override val roleId: Long,
    override val compCd: String
): DtoAllInOneBase