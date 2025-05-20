package kr.co.imoscloud.fetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.dto.*
import kr.co.imoscloud.entity.system.Company
import kr.co.imoscloud.entity.system.Menu
import kr.co.imoscloud.entity.system.UserRole
import kr.co.imoscloud.service.sysrtem.*

@DgsComponent
class SystemFetcher(
    private val userService: UserService,
    private val userRoleService: UserRoleService,
    private val menuService: MenuService,
    private val menuRoleService: MenuRoleService,
    private val companyService: CompanyService,
    private val noticeService: NoticeService,
) {

    @DgsMutation
    fun upsertUser(@InputArgument("req") req: UserInput) { userService.upsertUser(req) }

    @DgsMutation
    fun updateMyInfo(@InputArgument("req") req: UserInput) { userService.updateMyInfo(req) }

    @DgsQuery
    fun existLoginId(@InputArgument("req") req: ExistLoginIdRequest): Boolean = userService.existLoginId(req)

    @DgsQuery
    fun getUserGroup(@InputArgument("req") req: UserGroupRequest?): List<UserSummery?> = userService.getUserGroupByCompany(req)

    @DgsQuery
    fun getUserSummery(@InputArgument("loginId") loginId: String): UserSummery = userService.getUserSummery(loginId)

    @DgsQuery
    fun getUserDetail(@InputArgument("id") id: Long): UserDetail = userService.getUserDetail(id)

    @DgsMutation
    fun deleteUser(@InputArgument("id") id: Long) = userService.deleteUser(id)

    @DgsMutation
    fun resetPwd(@InputArgument("id")  id: Long) = userService.resetPassword(id)

    @DgsMutation
    fun changePwd(
        @InputArgument("id") id: Long,
        @InputArgument("currentPassword") currentPassword: String,
        @InputArgument("newPassword") newPassword: String
    ): String = userService.changePassword(id, currentPassword, newPassword)




    @DgsQuery
    fun getRoles(@InputArgument("req") req: RoleSearchRequest): List<UserRole> = userRoleService.getUserRoleGroup(req)

    @DgsQuery
    fun getRolesForSelect(): List<RoleSummery?> = userRoleService.getUserRoleSelect()

    @DgsMutation
    fun upsertUserRole(@InputArgument("req") req: UserRoleRequest): String = userRoleService.upsertUserRole(req)

    @DgsMutation
    fun deleteUserRole(@InputArgument("roleId") roleId: Long) = userRoleService.deleteUserRole(roleId)





    @DgsQuery
    fun getMenuRoleGroup(@InputArgument("roleId") roleId: Long) = menuRoleService.getMenuRoleGroup(roleId)

    @DgsQuery
    fun getMenuRole(@InputArgument("menuId") menuId: String) = menuRoleService.getMenuRole(menuId)

    @DgsMutation
    fun upsertMenuRole(@InputArgument("list") list: List<MenuRoleDto>) = menuRoleService.upsertMenuRole(list)





    @DgsQuery
    fun getMenus(@InputArgument("menuId") menuId: String?, @InputArgument("menuName") menuName: String?): List<Menu>? =
        menuService.getMenus(menuId, menuName)

    @DgsMutation
    fun upsertMenus(@InputArgument("req") req: MenuRequest): String = menuService.upsertMenus(req)

    @DgsMutation
    fun deleteMenu(@InputArgument("id") id: Long): String = menuService.deleteMenu(id)





    @DgsQuery
    fun getCompaniesForSelect(): List<CompanySummery?> = companyService.getCompaniesForSelect()

    @DgsQuery
    fun getCompanies(@InputArgument("req") req: CompanySearchCondition): List<Company> = companyService.getCompanies(req)

    @DgsQuery
    fun getCompanyDetails(): Company = companyService.getCompanyDetails()

    @DgsMutation
    fun upsertCompany(@InputArgument("req") req: CompanyDto) = companyService.upsertCompany(req)

    @DgsMutation
    fun deleteCompany(@InputArgument("id") id: Long) = companyService.deleteCompany(id)



    

    @DgsQuery
    fun getALlNotice(@InputArgument("req") req: NoticeSearchRequest) = noticeService.getALlNotice(req)

    @DgsMutation
    fun upsertNotice(@InputArgument("req") req: UpsertNoticeRequest) = noticeService.upsertNotice(req)

    @DgsMutation
    fun deleteNotice(@InputArgument("noticeId") noticeId: Long) = noticeService.deleteNotice(noticeId)

    @DgsMutation
    fun upReadCountForNotice(@InputArgument("noticeId") noticeId: Long) = noticeService.upReadCountForNotice(noticeId)
}
