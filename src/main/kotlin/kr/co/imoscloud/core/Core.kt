package kr.co.imoscloud.core

import kr.co.imoscloud.dto.*
import kr.co.imoscloud.entity.system.Company
import kr.co.imoscloud.entity.system.MenuRole
import kr.co.imoscloud.entity.system.User
import kr.co.imoscloud.entity.system.UserRole
import kr.co.imoscloud.repository.system.CompanyRepository
import kr.co.imoscloud.repository.system.*
import kr.co.imoscloud.security.UserPrincipal
import org.springframework.stereotype.Component

@Component
class Core(
    userRepo: UserRepository,
    roleRepo: UserRoleRepository,
    companyRepo: CompanyRepository,
    menuRoleRepo: MenuRoleRepository
): AbstractInitialSetting(userRepo, roleRepo, companyRepo, menuRoleRepo) {
    override fun getAllUsersDuringInspection(indies: List<String?>): MutableMap<String, UserSummery?> {
        val userList: List<User> = if (indies.size == 1) {
            userRepo.findByLoginId(indies.first()).map(::listOf)!!.orElseGet { emptyList<User>() }
        } else userRepo.findAllByLoginIdIn(indies)

        return userList.associate { it.loginId to userToSummery(it) }.toMutableMap()
    }

    override fun getAllRolesDuringInspection(indies: List<Long?>): MutableMap<Long, RoleSummery?> {
        val roleList: List<UserRole> = if (indies.size == 1 && indies.first() != null) {
            roleRepo.findById(indies.first()!!).map(::listOf).orElseGet { emptyList<UserRole>() }
        } else roleRepo.findAllByRoleIdIn(indies)

        return roleList.associate { it.roleId to roleToSummery(it) }.toMutableMap()
    }

    override fun getAllCompanyDuringInspection(indies: List<String?>): MutableMap<String, CompanySummery?> {
        val companyList: List<Company> = if (indies.size == 1) {
            companyRepo.findByCompCd(indies.first()).map(::listOf).orElseGet { emptyList<Company>() }
        } else companyRepo.findAllByCompCdIn(indies)

        return companyList.associate { it.compCd to companyToSummery(it) }.toMutableMap()
    }

    override fun getMenuRoleDuringInspection(roleId: Long, menuId: String?): List<MenuRole> {
        return menuId
            ?.let { menuRoleRepo.findByRoleIdAndMenuId(roleId, menuId)?.let { listOf(it) } }
            ?:run { menuRoleRepo.findAllByRoleId(roleId) }
    }

    fun getUserFromInMemory(loginId: String): UserSummery? = getAllUserMap(ExistLoginIdRequest(loginId))[loginId]

    fun getUserRoleFromInMemory(roleId: Long): RoleSummery? = getAllRoleMap(OnlyRoleIdReq(roleId))[roleId]

    fun getCompanyFromInMemory(compCd: String): CompanySummery? = getAllCompanyMap(OnlyCompanyIdReq(compCd))[compCd]

    fun getUserGroupByCompCd(loginUser: UserPrincipal): List<UserSummery?> {
        return if (getIsInspect()) {
            userRepo.findAllByCompCdAndFlagActiveIsTrue(loginUser.compCd)
                .map { userToSummery(it) }
        } else {
            getAllUserMap(loginUser)
                .filterValues { it?.compCd == loginUser.compCd }
                .values.toList()
                .sortedBy { it?.id }
        }
    }

    fun getRoleGroupByCompCd(loginUser: UserPrincipal): List<RoleSummery?> {
        return if (getIsInspect()) {
            roleRepo.getRolesByCompanyForExceptDev(loginUser.compCd).map { roleToSummery(it) }
        } else {
            getAllRoleMap(loginUser)
                .filterValues { v -> (v?.compCd == loginUser.compCd || v?.compCd == "default" ) }
                .values.toList()
                .sortedByDescending { it?.priorityLevel }
        }
    }

    fun <T> extractReferenceDataMaps(req: List<T>): SummaryMaps {
        val indiesMap: Map<String, List<Any>> = extractAllFromRequest(req)
        val userIdList = indiesMap["userIdList"]?.filterIsInstance<String>()
        val roleIdList = indiesMap["roleIdList"]?.filterIsInstance<Long>()
        val companyIdList = indiesMap["companyIdList"]?.filterIsInstance<String>()

        return SummaryMaps(
            userIdList?.let { getAllUserMapByIndies(it) },
            roleIdList?.let { getAllRoleMapByIndies(it) },
            companyIdList?.let { getAllCompanyMapByIndies(it) }
        )
    }

    fun isDeveloper(loginUser: UserPrincipal): Boolean =
        getUserRoleFromInMemory(loginUser.roleId)
            ?.let { it.priorityLevel == 5 }
            ?: throw IllegalArgumentException("권한 정보를 찾을 수 없습니다. ")

    fun isAdminOrHigher(loginUser: UserPrincipal): Boolean =
        getUserRoleFromInMemory(loginUser.roleId)
            ?.let { it.priorityLevel >= 3 }
            ?: throw IllegalArgumentException("권한 정보를 찾을 수 없습니다. ")

    fun validatePriorityIsHigherThan(roleId: Long, loginUser: UserPrincipal): Unit {
        val roleMap = getAllRoleMap(OnlyRoleIdReq(roleId), loginUser)

        try {
            val targetRole = roleMap[roleId]!!
            val loginUserRole = roleMap[loginUser.roleId]!!
            if (targetRole.priorityLevel > loginUserRole.priorityLevel) {
                val msg = "권한 레벨이 부족합니다. ${targetRole.roleName} 또는 그에 준하거나 이상의 권한이 필요합니다."
                throw IllegalArgumentException(msg)
            }
        } catch (e: NullPointerException) {
            throw IllegalArgumentException("대상: $roleId 또는 로그인 유저: ${loginUser.roleId} 의 권한 정보가 존재하지 않습니다. ")
        }
    }
}