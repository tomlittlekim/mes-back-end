package kr.co.imoscloud.core

import kr.co.imoscloud.dto.*
import kr.co.imoscloud.entity.company.Company
import kr.co.imoscloud.entity.user.MenuRole
import kr.co.imoscloud.entity.user.User
import kr.co.imoscloud.entity.user.UserRole
import kr.co.imoscloud.repository.company.CompanyRepository
import kr.co.imoscloud.repository.user.MenuRoleRepository
import kr.co.imoscloud.repository.user.UserRepository
import kr.co.imoscloud.repository.user.UserRoleRepository
import kr.co.imoscloud.util.SecurityUtils
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

        return userList.associate {
            val summery = userToUserSummery(it)
            it.loginId to summery
        }.toMutableMap()
    }

    override fun getAllRolesDuringInspection(indies: List<Long>): MutableMap<Long, RoleSummery?> {
        val roleList: List<UserRole> = if (indies.size == 1) {
            roleRepo.findById(indies.first()).map(::listOf).orElseGet { emptyList<UserRole>() }
        } else roleRepo.findAllByRoleIdIn(indies)

        return roleList.associate {
            val summery = RoleSummery(it.roleName, it.priorityLevel)
            it.roleId to summery
        }.toMutableMap()
    }

    override fun getAllCompanyDuringInspection(indies: List<String>): MutableMap<String, CompanySummery?> {
        val companyList: List<Company> = if (indies.size == 1) {
            companyRepo.findByCompCd(indies.first()).map(::listOf).orElseGet { emptyList<Company>() }
        } else companyRepo.findAllByCompCdIn(indies)

        return companyList.associate {
            val summery = CompanySummery(it.id, it.companyName)
            it.compCd to summery
        }.toMutableMap()
    }

    override fun getMenuRoleDuringInspection(roleId: Long, menuId: String): MenuRole? {
        return menuRoleRepo.findByRoleIdAndMenuId(roleId, menuId)
    }

    fun <T> getUserFromInMemory(req: T): UserSummery {
        val index: String
        val userMap: Map<String, UserSummery?> = when (req) {
            is String -> { index = req; getAllUserMap(listOf(ExistLoginIdRequest(req))) }
            is User -> { index = req.loginId; getAllUserMap(listOf(req)) }
            else -> throw IllegalArgumentException("지원하지 않는 객체입니다. want: Long,UserRole")
        }
        return userMap[index] ?: throw IllegalArgumentException("User not found with loginId: $index")
    }

    fun <T> getUserRoleFromInMemory(req: T): RoleSummery {
        val index: Long
        val roleMap: Map<Long, RoleSummery?> = when (req) {
            is Long -> { index = req; getAllRoleMap(listOf(RoleInput(req))) }
            is UserRole -> { index = req.roleId; getAllRoleMap(listOf(req)) }
            else -> throw IllegalArgumentException("지원하지 않는 객체입니다. want: Long,UserRole")
        }
        return roleMap[index] ?: throw IllegalArgumentException("권한 정보가 존재하지 않습니다. ")
    }

    fun getUserGroupByCompCd(): List<UserSummery?> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return if (getIsInspect()) {
            userRepo.findAllBySiteAndCompCdAndFlagActiveIsTrue(loginUser.getSite(), loginUser.getCompCd())
                .map { userToUserSummery(it) }
        } else {
            val userMap = getAllUserMap(listOf(loginUser))
            return userMap[loginUser.getLoginId()]
                ?.takeIf { it.compCd == loginUser.getCompCd() }
                ?.let { listOf(it) }
                ?: emptyList()
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

    fun <T> userToUserResponse(any: T): UserResponse {
        val u = when (any) {
            is User -> userToUserSummery(any)
            is UserSummery -> any
            else -> throw IllegalArgumentException("메모리에 유저에 대한 정보 누락이거나 지원하지 않는 객체입니다. ")
        }

        val role = getUserRoleFromInMemory(u.roleId)
        return UserResponse(u.loginId,u.username?:"",u.departmentId,u.positionId,role.roleName,u.flagActive)
    }
}