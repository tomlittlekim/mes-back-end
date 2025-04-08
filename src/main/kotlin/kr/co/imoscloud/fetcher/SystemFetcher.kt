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
import kr.co.imoscloud.service.sysrtem.companyService
import kr.co.imoscloud.service.sysrtem.MenuRoleService
import kr.co.imoscloud.service.sysrtem.MenuService

@DgsComponent
class SystemFetcher(
    private val userService: UserService,
    private val userRoleService: UserRoleService,
    private val menuService: MenuService,
    private val menuRoleService: MenuRoleService,
    private val companyService: companyService
) {

    @DgsMutation
    fun upsertUser(@InputArgument("req") req: UserInput) { userService.upsertUser(req) }

    @DgsQuery
    fun existLoginId(@InputArgument("req") req: ExistLoginIdRequest): Boolean = userService.existLoginId(req)

    @DgsQuery
    fun getUserGroup(@InputArgument("req") req: UserGroupRequest?): List<UserSummery?> = userService.getUserGroupByCompany(req)

    @DgsQuery
    fun getUserDetail(@InputArgument("id") id: Long): UserDetail = userService.getUserDetail(id)

    @DgsMutation
    fun deleteUser(@InputArgument("id") id: Long) = userService.deleteUser(id)

    @DgsMutation
    fun resetPwd(@InputArgument("id")  id: Long) = userService.resetPassword(id)





    @DgsQuery
    fun getRoles(): List<UserRole> = userRoleService.getUserRoleGroup()

    @DgsQuery
    fun getRolesForSelect(): List<RoleSummery?> = userRoleService.getUserRoleSelect()

    @DgsMutation
    fun upsertUserRole(req: UserRoleRequest): String = userRoleService.upsertUserRole(req)





    @DgsQuery
    fun getMenuRoleGroup() = menuRoleService.getMenuRoleGroup()

    @DgsQuery
    fun getMenuRole(@InputArgument("menuId") menuId: String) = menuRoleService.getMenuRole(menuId)





    @DgsQuery
    fun getMenus(): List<Menu> = menuService.getMenus()

    @DgsMutation
    fun upsertMenus(req: MenuRequest): String = menuService.upsertMenus(req)





    @DgsQuery
    fun getCompanySelect(): List<Company> = companyService.getCompanySelect()
}
