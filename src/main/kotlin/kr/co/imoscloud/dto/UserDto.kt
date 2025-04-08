package kr.co.imoscloud.dto

import kr.co.imoscloud.iface.DtoAllInOneBase
import kr.co.imoscloud.iface.DtoCompCdBase
import kr.co.imoscloud.iface.DtoLoginIdBase
import kr.co.imoscloud.iface.DtoRoleIdBase
import kr.co.imoscloud.iface.ResponseVO.ResponseBase

data class UserInput(
    val id: Long? = null,
    var site: String?=null,
    var compCd: String?=null,
    var loginId: String?=null,
    var userPwd: String?=null,
    var userName: String?=null,
    var userEmail: String?=null,
    var roleId: Long?=null,
    var phoneNum: String?=null,
    var departmentId: String?=null,
    var positionId: String?=null,
    var flagActive: Boolean?=null
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
    var priorityLevel: Int?=null
): ResponseBase, DtoRoleIdBase

data class LoginRequest(val userId: String, val userPwd: String)

data class ExistLoginIdRequest(
    override val loginId: String
): DtoLoginIdBase

data class UserResponse(
    val id: Long,
    override val loginId: String,
    val userName: String,
    var departmentName: String?,
    var position: String?,
    val authorityName: String,
    val flagActive: String
): DtoLoginIdBase

data class UserSummery(
    val id: Long,
    val site: String,
    override val compCd: String,
    val userName: String?=null,
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

data class UserDetail(
    val id: Long,
    override val loginId: String,
    val userName: String,
    var departmentName: String?,
    var position: String?,
    val authorityName: String,
    val email: String?,
    val phoneNumber: String?,
    val flagActive: String
): DtoLoginIdBase

data class UserGroupRequest(
    var userName: String? = null,
    var departmentId: String? = null,
    var positionId: String? = null,
    var roleId: Long? = null
)

data class OnlyRoleIdReq(
    override val roleId: Long,
): DtoRoleIdBase

data class RoleSummery(
    override val roleId: Long,
    override val compCd: String,
    val roleName: String,
    val priorityLevel: Int,
): DtoRoleIdBase, DtoCompCdBase

data class UserRoleRequest(
    val site: String?=null,
    val roleId: Long?=null,
    val fixRoleId: Long,
    val roleName: String?=null,
    val compCd: String?=null,
    val flagDeFault: Boolean?=null,
)

data class MenuRequest(
    val id: Long?=null,
    val menuId: String? = null,
    val upMenuId: String? = null,
    val menuName: String? = null,
    val flagSubscribe: Boolean?=null,
    val sequence: Int? = null,
    val flagActive: Boolean = true
)

data class MenuRoleDto(
    var id: Long?=null,
    var roleId: Long?=null,
    var menuId: String,
    var isOpen: Boolean?=null,
    var isDelete: Boolean?=null,
    var isInsert: Boolean?=null,
    var isAdd: Boolean?=null,
    var isPopup: Boolean?=null,
    var isPrint: Boolean?=null,
    var isSelect: Boolean?=null,
    var isUpdate: Boolean?=null
)