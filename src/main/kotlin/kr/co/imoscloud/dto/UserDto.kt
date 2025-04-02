package kr.co.imoscloud.dto

import kr.co.imoscloud.iface.DtoAllInOneBase
import kr.co.imoscloud.iface.DtoLoginIdBase
import kr.co.imoscloud.iface.DtoRoleIdBase
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

data class ExistLoginIdRequest(
    override val loginId: String
): DtoLoginIdBase

data class UserResponse(
    override val loginId: String,
    val username: String,
    var departmentNm: String?,
    var positionNm: String?,
    val roleNm: String,
    val flagActive: Boolean
): DtoLoginIdBase

data class UserSummery(
    val id: Long,
    val site: String,
    override val compCd: String,
    val username: String?=null,
    override val loginId: String,
    val userPwd: String,
    val imagePath: String?=null,
    override val roleId: Long,
    val userEmail: String?=null,
    val phoneNum: String?=null,
    val departmentId: String?=null,
    val positionId: String?=null,
    val flagActive: Boolean,
): DtoAllInOneBase

data class RoleInput(
    override val roleId: Long,
): DtoRoleIdBase

data class RoleSummery(
    val roleName: String,
    val priorityLevel: Int?,
)