package kr.co.imoscloud.dto

import kr.co.imoscloud.iface.*
import kr.co.imoscloud.iface.ResponseVO.ResponseBase

data class UserInput(
    val id: Long? = null,
    var site: String?=null,
    var compCd: String?=null,
    var loginId: String?=null,
    var userPwd: String?=null,
    var imagePath: String?=null,
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
    val site: String?=null,
    val compCd: String?=null,
    val id: Long?=null,
    val loginId: String?=null,
    var userNm: String?=null,
    var email: String?=null,
    var roleId: Long?=null,
    var roleNm: String?=null,
    var priorityLevel: Int?=null
): ResponseBase

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
    val site: String,
    override val compCd: String,
    val userName: String,
    override val loginId: String,
    val userPwd: String,
    val imagePath: String?=null,
    override val roleId: Long,
    val userEmail: String?,
    val phoneNum: String?,
    val departmentId: String?=null,
    var departmentName: String?,
    val positionId: String?=null,
    var positionName: String?,
    val authorityName: String,
    val flagActive: Boolean,
): DtoAllInOneBase

data class UserGroupRequest(
    var userName: String? = null,
    var departmentId: String? = null,
    var positionId: String? = null,
    var roleId: Long? = null
)

data class OnlyRoleIdReq(
    override val roleId: Long,
): DtoRoleIdBase

data class OnlyCompanyIdReq(
    override val compCd: String
): DtoCompCdBase

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
    val flagDefault: Boolean?=null,
    val sequence: Int?=0,
)

data class MenuRequest(
    val id: Long?=null,
    val menuId: String? = null,
    val upMenuId: String? = null,
    val menuName: String? = null,
    val flagSubscribe: Boolean?=null,
    val sequence: Int? = null,
    val flagActive: Boolean = true,

    // 신규 메뉴 생성 시 default 값을 위함 필드들
    var isOpen: Boolean?=null,
    var isDelete: Boolean?=null,
    var isInsert: Boolean?=null,
    var isAdd: Boolean?=null,
    var isPopup: Boolean?=null,
    var isPrint: Boolean?=null,
    var isSelect: Boolean?=null,
    var isUpdate: Boolean?=null
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

data class RoleSearchRequest(
    val site: String? = null,
    val compCd: String? = null,
    val priorityLevel: Int? = null,
)

data class UpsertNoticeRequest(
    val noticeId: Long? = null,
    val noticeTitle: String? = null,
    val attachmentPath: String? = null,
    val noticeContents: String? = null,
    val priorityLevel: Int? = null,
    val noticeTtl: String? = null,
)

data class NoticeSearchRequest(
    val fromDate: String? = null,
    val toDate: String? = null,
)