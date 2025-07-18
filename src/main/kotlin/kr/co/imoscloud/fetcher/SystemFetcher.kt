package kr.co.imoscloud.fetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.dto.*
import kr.co.imoscloud.entity.system.Company
import kr.co.imoscloud.entity.system.Menu
import kr.co.imoscloud.entity.system.UserRole
import kr.co.imoscloud.model.kpisetting.*
import kr.co.imoscloud.service.system.*

@DgsComponent
class SystemFetcher(
    private val userService: UserService,
    private val userRoleService: UserRoleService,
    private val menuService: MenuService,
    private val menuRoleService: MenuRoleService,
    private val companyService: CompanyService,
    private val noticeService: NoticeService,
    private val kpiSettingService: KpiSettingService
) {

    @DgsMutation
    fun upsertUser(@InputArgument("req") req: UserInput) { userService.upsertUser(req) }

    @DgsMutation
    fun updateMyInfo(@InputArgument("req") req: UserInput) { userService.updateMyInfo(req) }

    @DgsQuery
    fun existLoginId(@InputArgument("loginId") loginId: String): Boolean = userService.existLoginId(loginId)

    @DgsQuery
    fun getUserGroup(@InputArgument("req") req: UserGroupRequest?): List<UserSummery?> = userService.getUserGroupByCompany(req)

    @DgsQuery
    fun getUserSummery(@InputArgument("loginId") loginId: String): UserSummery = userService.getUserSummery(loginId)

    @DgsQuery
    fun getUserDetail(@InputArgument("loginId") loginId: String): UserDetail = userService.getUserDetail(loginId)

    @DgsMutation
    fun deleteUser(@InputArgument("loginId") loginId: String) = userService.deleteUser(loginId)

    @DgsMutation
    fun resetPwd(@InputArgument("loginId") loginId: String) = userService.resetPassword(loginId)

    @DgsMutation
    fun changePwd(
        @InputArgument("loginId") loginId: String,
        @InputArgument("currentPassword") currentPassword: String,
        @InputArgument("newPassword") newPassword: String
    ): String = userService.changePassword(loginId, currentPassword, newPassword)

    @DgsMutation
    fun resetPasswordByUserInfo(
        @InputArgument("userName") userName: String,
        @InputArgument("phoneNum") phoneNum: String,
    ): Boolean = userService.resetPasswordByUserInfo(userName, phoneNum)




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
    fun deleteMenu(@InputArgument("menuId") menuId: String): String = menuService.deleteMenu(menuId)





    @DgsQuery
    fun getCompaniesForSelect(): List<CompanySummery?> = companyService.getCompaniesForSelect()

    @DgsQuery
    fun getCompanies(@InputArgument("req") req: CompanySearchCondition): List<Company> = companyService.getCompanies(req)

    @DgsQuery
    fun getCompanyDetails(): Company = companyService.getCompanyDetails()

    @DgsMutation
    fun upsertCompany(@InputArgument("req") req: CompanyDto) = companyService.upsertCompany(req)

    @DgsMutation
    fun deleteCompany(@InputArgument("compCd") compCd: String) = companyService.deleteCompany(compCd)



    

    @DgsQuery
    fun getALlNotice(@InputArgument("req") req: NoticeSearchRequest) = noticeService.getALlNotice(req)

    @DgsMutation
    fun upsertNotice(@InputArgument("req") req: UpsertNoticeRequest) = noticeService.upsertNotice(req)

    @DgsMutation
    fun deleteNotice(@InputArgument("noticeId") noticeId: Long) = noticeService.deleteNotice(noticeId)

    @DgsMutation
    fun upReadCountForNotice(@InputArgument("noticeId") noticeId: Long) = noticeService.upReadCountForNotice(noticeId)




    /* KPI 설정 관련 기능 */
    @DgsQuery
    fun getBranchCompanies(): List<BranchModel> = kpiSettingService.getBranchCompanies()

    @DgsQuery
    fun getKpiIndicators(): List<KpiIndicatorModel> = kpiSettingService.getKpiIndicators()

    @DgsQuery
    fun getKpiSubscriptions(): List<KpiSubscriptionModel> = kpiSettingService.getKpiSubscriptions()

    @DgsMutation
    fun saveKpiSettings(@InputArgument("settings") settings: List<Map<String, Any>>): KpiSettingResult {
        try {
            // 프론트엔드에서 보내는 KPISettingInput을 KpiSettingInput으로 변환
            val kpiSettings = settings.map { input ->
                KpiSettingInput(
                    site = input["site"] as String,
                    compCd = input["compCd"] as String,
                    kpiIndicatorCd = input["kpiIndicatorCd"] as String,
                    categoryId = input["categoryId"] as String,
                    targetValue = input["targetValue"] as? Double,
                    description = input["description"] as? String,
                    sort = (input["sort"] as? Int),
                    flagActive = (input["flagActive"] as? Boolean) ?: true
                )
            }
            
            return kpiSettingService.saveKpiSettings(kpiSettings)
        } catch (e: Exception) {
            e.printStackTrace()
            return KpiSettingResult(success = false, message = "KPI 설정 저장 중 오류가 발생했습니다: ${e.message}")
        }
    }
}
