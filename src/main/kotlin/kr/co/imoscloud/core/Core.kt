package kr.co.imoscloud.core

import kr.co.imoscloud.dto.*
import kr.co.imoscloud.entity.company.Company
import kr.co.imoscloud.entity.user.MenuRole
import kr.co.imoscloud.entity.user.User
import kr.co.imoscloud.entity.user.UserRole
import kr.co.imoscloud.iface.DtoLoginIdBase
import kr.co.imoscloud.iface.DtoRoleIdBase
import kr.co.imoscloud.repository.company.CompanyRepository
import kr.co.imoscloud.repository.user.*
import kr.co.imoscloud.security.UserPrincipal
import org.springframework.stereotype.Component

@Component
class Core(
    userRepo: UserRepository,
    roleRepo: UserRoleRepository,
    companyRepo: CompanyRepository,
    menuRoleRepo: MenuRoleRepository
): AbstractInitialSetting(userRepo, roleRepo, companyRepo, menuRoleRepo) {
    override fun getAllUsersDuringInspection(indies: List<String>): MutableMap<String, UserSummery?> {
        val userList: List<User> = if (indies.size == 1) {
            userRepo.findByLoginId(indies.first()).map(::listOf)!!.orElseGet { emptyList<User>() }
        } else userRepo.findAllByLoginIdIn(indies)

        return userList.associate { it.loginId to userToSummery(it) }.toMutableMap()
    }

    override fun getAllRolesDuringInspection(indies: List<Long>): MutableMap<Long, RoleSummery?> {
        val roleList: List<UserRole> = if (indies.size == 1) {
            roleRepo.findById(indies.first()).map(::listOf).orElseGet { emptyList<UserRole>() }
        } else roleRepo.findAllByRoleIdIn(indies)

        return roleList.associate { it.roleId to roleToSummery(it) }.toMutableMap()
    }

    override fun getAllCompanyDuringInspection(indies: List<String>): MutableMap<String, CompanySummery?> {
        val companyList: List<Company> = if (indies.size == 1) {
            companyRepo.findByCompCd(indies.first()).map(::listOf).orElseGet { emptyList<Company>() }
        } else companyRepo.findAllByCompCdIn(indies)

        return companyList.associate { it.compCd to CompanySummery(it.id, it.companyName) }.toMutableMap()
    }

    override fun getMenuRoleDuringInspection(roleId: Long, menuId: String): MenuRole? {
        return menuRoleRepo.findByRoleIdAndMenuId(roleId, menuId)
    }

    fun <T> getUserFromInMemory(req: T): UserSummery {
        val index: String
        val userMap: Map<String, UserSummery?> = when {
            req is String -> { index = req; getAllUserMap(listOf(ExistLoginIdRequest(req))) }
            req is DtoLoginIdBase -> { index = req.loginId; getAllUserMap(listOf(req)) }
            else -> throw IllegalArgumentException("지원하지 않는 객체입니다. want: Long,UserRole")
        }
        return userMap[index] ?: throw IllegalArgumentException("User not found with loginId: $index")
    }

    fun <T> getUserRoleFromInMemory(req: T): RoleSummery {
        val index: Long
        val roleMap: Map<Long, RoleSummery?> = when  {
            req is Long -> { index = req; getAllRoleMap(listOf(RoleInput(req))) }
            req is DtoRoleIdBase -> { index = req.roleId; getAllRoleMap(listOf(req)) }
            else -> throw IllegalArgumentException("지원하지 않는 객체입니다. want: Long,UserRole")
        }
        return roleMap[index] ?: throw IllegalArgumentException("권한 정보가 존재하지 않습니다. ")
    }

    fun getUserGroupByCompCd(loginUser: UserPrincipal): List<UserSummery?> {
        return if (getIsInspect()) {
            userRepo.findAllBySiteAndCompCdAndFlagActiveIsTrue(loginUser.getSite(), loginUser.compCd)
                .map { userToSummery(it) }
        } else {
            val userMap = getAllUserMap(listOf(loginUser))
            return userMap.filterValues { it?.compCd == loginUser.compCd }.values.toList()
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

    fun isDeveloper(loginUser: UserPrincipal): Boolean {
        val roleSummery = getUserRoleFromInMemory(loginUser)
        return roleSummery.priorityLevel == 5
    }

    fun isAdminOrHigher(loginUser: UserPrincipal): Boolean {
        val roleSummery = getUserRoleFromInMemory(loginUser)
        return roleSummery.priorityLevel!! >= 3
    }
}