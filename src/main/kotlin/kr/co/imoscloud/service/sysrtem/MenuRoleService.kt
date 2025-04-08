package kr.co.imoscloud.service.sysrtem

import kr.co.imoscloud.core.Core
import kr.co.imoscloud.dto.MenuRequest
import kr.co.imoscloud.dto.MenuRoleResponse
import kr.co.imoscloud.entity.user.MenuRole
import kr.co.imoscloud.repository.system.MenuRepository
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service

@Service
class MenuRoleService(
    private val core: Core,
    private val menuRepo: MenuRepository
) {

    fun getMenuRoleGroup(): List<MenuRole> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return core.getAllMenuRoleByRoleId(loginUser.roleId)
    }

    fun getMenuRole(menuId: String): MenuRole {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return core.getMenuRole(loginUser.roleId, menuId)
            ?: throw IllegalArgumentException("Menu에 대한 권한 정보가 존재하지 않습니다. ")
    }

    fun getInitialMenuRole(roleId: Long): List<MenuRoleResponse> =
        menuRepo.findAll().map { menu -> MenuRoleResponse(roleId = roleId, menuId = menu.menuId) }

    fun upsertMenuRole(req: MenuRequest): String {
        return ""
    }
}