package kr.co.imoscloud.service

import kr.co.imoscloud.core.Core
import kr.co.imoscloud.entity.user.MenuRole
import kr.co.imoscloud.entity.user.UserRole
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service

@Service
class UserRoleService(
    val core: Core
) {

    fun getUserRoleGroup(): List<UserRole> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val result = core.roleRepo.getRolesByCompany(loginUser.compCd)
        return result
    }

    fun getMenuRoleGroup(): List<MenuRole> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return core.getAllMenuRoleByRoleId(loginUser.roleId)
    }
}