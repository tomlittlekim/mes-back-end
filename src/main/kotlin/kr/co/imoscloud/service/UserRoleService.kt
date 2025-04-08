package kr.co.imoscloud.service

import kr.co.imoscloud.core.Core
import kr.co.imoscloud.dto.RoleSummery
import kr.co.imoscloud.entity.user.MenuRole
import kr.co.imoscloud.entity.user.UserRole
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.data.crossstore.ChangeSetPersister
import org.springframework.stereotype.Service

@Service
class UserRoleService(
    val core: Core
) {

    fun getUserRoleSelect(): List<RoleSummery?> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        return if (core.isDeveloper(loginUser)) {
            core.getAllRoleMap(loginUser).values.toList()
        } else {
            core.getRoleGroupByCompCd(loginUser)
        }
    }

    fun getUserRoleGroup(): List<UserRole> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        return if (core.isDeveloper(loginUser)) {
            core.roleRepo.findAllByFlagActiveIsTrue()
        } else {
            core.roleRepo.getRolesByCompany(loginUser.compCd)
        }
    }

    fun getMenuRoleGroup(): List<MenuRole> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return core.getAllMenuRoleByRoleId(loginUser.roleId)
    }

    fun getMenuRole(menuId: String): MenuRole {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return core.getMenuRole(loginUser.roleId, menuId)
            ?: throw IllegalArgumentException("Menu에 대한 권한 정보가 존재하지 않습니다. ")
    }

    fun upsertUserRole(userRole: UserRole) {

    }
}