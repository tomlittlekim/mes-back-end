package kr.co.imoscloud.service.sysrtem

import jakarta.transaction.Transactional
import kr.co.imoscloud.core.MenuCacheManager
import kr.co.imoscloud.core.MenuRoleCacheManager
import kr.co.imoscloud.core.UserRoleCacheManager
import kr.co.imoscloud.dto.MenuRoleDto
import kr.co.imoscloud.entity.system.Menu
import kr.co.imoscloud.entity.system.MenuRole
import kr.co.imoscloud.util.AuthLevel
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service

@Service
class MenuRoleService(
    private val rcm: UserRoleCacheManager,
    private val mcm: MenuCacheManager,
    val mrcm: MenuRoleCacheManager
) {
    private val menuRoleRepo get() = mrcm.menuRoleRepo

    fun getMenuRoleGroup(roleId: Long): List<Any> {
        val allMenuMap = mcm.getMenus(listOf("string"))
        val allMenu: List<Menu> = allMenuMap.mapNotNull { (_, v) -> v }.toList()

        val menuRoleList = mrcm.getMenuRoles(roleId).map { it.toDto(roleId, allMenuMap[it.menuId]?.upMenuId) }

        val defaultMenuRoles =
            if (menuRoleList.size < allMenu.size) {
                val savedMenuIds = menuRoleList.map { it.menuId }
                allMenu
                    .filter { menu -> !savedMenuIds.contains(menu.menuId) }
                    .map { it.toDto(roleId, allMenuMap[it.menuId]?.upMenuId) }
            } else null

        return defaultMenuRoles?.let { menuRoleList + defaultMenuRoles } ?: menuRoleList
    }

    fun getMenuRole(menuId: String): MenuRole {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return mrcm.getMenuRole(loginUser.roleId, menuId)
            ?: throw IllegalArgumentException("Menu에 대한 권한 정보가 존재하지 않습니다. ")
    }

    @AuthLevel(minLevel = 3)
    @Transactional
    fun upsertMenuRole(list: List<MenuRoleDto>): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val ids = list.mapNotNull { it.id }
        val menuRoleMap = if (ids.isEmpty()) emptyMap() else menuRoleRepo.findByIdIn(ids).associateBy { it.id }

        var upsertStr: String?=null
        val menuRoleList: List<MenuRole> =
            try {
                list.map { req ->
                    req.roleId?.let { roleId -> rcm.validatePriorityIsHigherThan(roleId, loginUser) }

                    menuRoleMap[req.id]
                        ?.modify(req)
                        ?:run {
                            upsertStr = "수정"
                            MenuRole.create(req)
                        }
                }
            } catch (e: NullPointerException) {
                throw IllegalArgumentException("메뉴 권한 생성 시 필요한 정보가 부족합니다. ")
            }

        mrcm.saveAllAndSyncCache(menuRoleList)
        return "메뉴 권한 ${upsertStr} 성공"
    }

    fun refreshCategoryIfParentChanged(menu: Menu, changedUpMenuId: String?): Unit {
        if (menu.upMenuId != changedUpMenuId) {
            val flagCategory = changedUpMenuId.isNullOrBlank()

            val buildMap: Map<String, List<MenuRole>> = mrcm.groupByKeySelector { it.menuId }
            val values: List<MenuRole>? = buildMap[menu.menuId]

            values?.let {
                val updateList = values.map { mr -> mr.modify(flagCategory) }
                mrcm.saveAllAndSyncCache(updateList)
            }
        }
    }

    fun deleteAllByMenuId(menuId: String): Unit {
        val buildMap: Map<String, List<MenuRole>> = mrcm.groupByKeySelector { it.menuId }
        val values: List<MenuRole>? = buildMap[menuId]
        values?.let { mrcm.softDeleteAndSyncCache(values) }
    }
}