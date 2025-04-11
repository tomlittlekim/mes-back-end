package kr.co.imoscloud.fetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.dto.*
import kr.co.imoscloud.entity.system.Company
import kr.co.imoscloud.entity.system.Menu
import kr.co.imoscloud.entity.system.UserRole
import kr.co.imoscloud.service.sysrtem.UserRoleService
import kr.co.imoscloud.service.sysrtem.UserService
import kr.co.imoscloud.service.sysrtem.CompanyService
import kr.co.imoscloud.service.sysrtem.MenuRoleService
import kr.co.imoscloud.service.sysrtem.MenuService

@DgsComponent
class SystemFetcher(
    private val userService: UserService,
    private val userRoleService: UserRoleService,
    private val menuService: MenuService,
    private val menuRoleService: MenuRoleService,
    private val companyService: CompanyService
) {

    @DgsMutation
    fun upsertUser(@InputArgument("req") req: UserInput) { userService.upsertUser(req) }

    @DgsQuery
    fun existLoginId(@InputArgument("req") req: ExistLoginIdRequest): Boolean = userService.existLoginId(req)

    @DgsQuery
    fun getUserGroup(@InputArgument("req") req: UserGroupRequest?): List<UserSummery?> = userService.getUserGroupByCompany(req)

    @DgsQuery
    fun getUserSummery(@InputArgument("loginId") loginId: String): UserSummery = userService.getUserSummery(loginId)

    @DgsQuery
    fun getUserDetail(@InputArgument("loginId") loginId: String): UserDetail = userService.getUserDetail(loginId)

    @DgsMutation
    fun deleteUser(@InputArgument("id") id: Long) = userService.deleteUser(id)

    @DgsMutation
    fun resetPwd(@InputArgument("id")  id: Long) = userService.resetPassword(id)





    @DgsQuery
    fun getRoles(): List<UserRole> = userRoleService.getUserRoleGroup()

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
    fun getMenus(@InputArgument("menuId") menuId: String?, @InputArgument("menuName") menuName: String?): List<Menu> =
        menuService.getMenus(menuId, menuName)

    @DgsMutation
    fun upsertMenus(@InputArgument("req") req: MenuRequest): String = menuService.upsertMenus(req)

    @DgsMutation
    fun deleteMenu(@InputArgument("id") id: Long): String = menuService.deleteMenu(id)




    @DgsQuery
    fun getCompaniesForSelect(): List<CompanySummery?> = companyService.getCompaniesForSelect()

    @DgsQuery
    fun getCompanies(): List<Company> = companyService.getCompanies()

    @DgsQuery
    fun getCompanyDetails(): Company = companyService.getCompanyDetails()

    @DgsMutation
    fun upsertCompany(@InputArgument("req") req: CompanyDto) = companyService.upsertCompany(req)

    @DgsMutation
    fun deleteCompany(@InputArgument("id") id: Long) = companyService.deleteCompany(id)
}
