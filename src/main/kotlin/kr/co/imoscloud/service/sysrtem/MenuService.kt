package kr.co.imoscloud.service.sysrtem

import jakarta.transaction.Transactional
import kr.co.imoscloud.core.MenuCacheManager
import kr.co.imoscloud.core.MenuRoleCacheManager
import kr.co.imoscloud.core.UserRoleCacheManager
import kr.co.imoscloud.dto.MenuRequest
import kr.co.imoscloud.entity.system.Menu
import kr.co.imoscloud.entity.system.MenuRole
import kr.co.imoscloud.util.AuthLevel
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service

@Service
class MenuService(
    private val rcm: UserRoleCacheManager,
    val mcm: MenuCacheManager,
    val menuRoleService: MenuRoleService,
) {
    private val roleRepo get() = rcm.roleRepo
    private val mrcm get() = menuRoleService.mrcm

    fun getMenus(menuId: String?, menuName: String?): List<Menu>? {
        val menuMap =  mcm.getMenus(listOf("string"))
        val menuList = menuMap.mapNotNull { (_, v) -> v }.toList()

        return if (menuId==null && menuName==null) menuList
        else {
            val menu = menuId?.let { menuMap[it]?.let { menu -> listOf(menu) } } ?: menuList
            menuName?.let { menu.filter { menu -> menu.menuName.contains(it) } }
        }
    }

    @AuthLevel(minLevel = 5)
    @Transactional
    fun upsertMenus(req: MenuRequest): String =
        try {
            val menu: Menu = req.id
                ?.let { id ->
                    mcm.associateByKeySelector<String, Menu?, Long> { it?.id }[id]
                        ?.let { menu: Menu ->
                            menuRoleService.refreshCategoryIfParentChanged(menu, req.upMenuId)
                            menu.modify(req)
                        }
                        ?: throw IllegalArgumentException("수정하려는 대상이 존재하지 않습니다. ")
                }
                ?:run {
                    val newMenu = Menu.create(req)

                    val loginUser = SecurityUtils.getCurrentUserPrincipal()
                    val roleMap = rcm.getUserRoles(listOf(loginUser.roleId))

                    val roleIds = if (roleMap.size == 1) roleRepo.getAllRoleIds()
                    else roleMap.values.mapNotNull { it?.roleId }

                    val menuRoleList = roleIds.map { id -> MenuRole.initial(id, newMenu) }
                    mrcm.saveAllAndSyncCache(menuRoleList)

                    newMenu
                }

            mcm.saveAllAndSyncCache(listOf(menu))
            "${menu.menuName} 메뉴 수정 성공"
        } catch (e: NullPointerException) {
            throw IllegalArgumentException("신규 메뉴 생성 실패. : 매개변수 값을 비어있습니다. ")
        }

    @AuthLevel(minLevel = 5)
    @Transactional
    fun deleteMenu(menuId: String): String {
        val menu = mcm.getMenu(menuId)
            ?.let { m -> mcm.deleteAndSyncCache(m) }
            ?: throw IllegalArgumentException("삭제하려는 메뉴가 존재하지 않습니다. ")

        menuRoleService.deleteAllByMenuId(menu.menuId)
        return "${menu.menuName} 및 해당 메뉴에 대한 권한들 삭제 성공"
    }
}