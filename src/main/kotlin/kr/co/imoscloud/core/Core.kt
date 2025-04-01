package kr.co.imoscloud.core

import kr.co.imoscloud.dto.CompanySummery
import kr.co.imoscloud.dto.RoleInput
import kr.co.imoscloud.dto.RoleSummery
import kr.co.imoscloud.dto.TestAllInOneDto
import kr.co.imoscloud.entity.company.Company
import kr.co.imoscloud.entity.user.MenuRole
import kr.co.imoscloud.entity.user.User
import kr.co.imoscloud.entity.user.UserRole
import kr.co.imoscloud.repository.company.CompanyRepository
import kr.co.imoscloud.repository.user.MenuRoleRepository
import kr.co.imoscloud.repository.user.UserRepository
import kr.co.imoscloud.repository.user.UserRoleRepository
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
        } else roleRepo.findAllByIdIn(indies)

        return roleList.associate {
            val summery = RoleSummery(it.roleName, it.priorityLevel)
            it.id to summery
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

    fun getUserRoleFromInMemory(user: User): RoleSummery {
        val req = RoleInput(user.roleId)
        val roleMap: Map<Long, RoleSummery?> = getAllRoleMap(listOf(req))
        return roleMap[req.roleId] ?: throw IllegalArgumentException("권한 정보가 존재하지 않습니다. ")
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
}