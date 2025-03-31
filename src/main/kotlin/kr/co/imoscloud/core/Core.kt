package kr.co.imoscloud.core

import kr.co.imoscloud.dto.CompanySummery
import kr.co.imoscloud.dto.RoleInput
import kr.co.imoscloud.dto.RoleSummery
import kr.co.imoscloud.dto.TestAllInOneDto
import kr.co.imoscloud.entity.company.Company
import kr.co.imoscloud.entity.user.User
import kr.co.imoscloud.entity.user.UserRole
import kr.co.imoscloud.repository.company.CompanyRepository
import kr.co.imoscloud.repository.user.UserRepository
import kr.co.imoscloud.repository.user.UserRoleRepository
import org.springframework.stereotype.Component

@Component
class Core(
    userRepo: UserRepository,
    roleRepo: UserRoleRepository,
    companyRepo: CompanyRepository,
): AbstractInitialSetting(userRepo, roleRepo, companyRepo) {
    override fun getAllUsersDuringInspection(indies: List<Long>): MutableMap<Long, String?> {
        val userList: List<User> = if (indies.size == 1) {
            userRepo.findById(indies.first()).map(::listOf)!!.orElseGet { emptyList<User>() }
        } else userRepo.findAllByIdIn(indies)

        return userList.associate { it.id to it.userName }.toMutableMap()
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

    fun getUserRoleFromInMemory(user: User): RoleSummery {
        val req = RoleInput(user.roleId)
        val test = TestAllInOneDto(user.id, user.roleId, user.compCd)
        val roleMap: Map<Long, RoleSummery?> = getAllRoleMap(listOf(test))
        return roleMap[req.roleId] ?: throw IllegalArgumentException("권한 정보가 존재하지 않습니다. ")
    }
}