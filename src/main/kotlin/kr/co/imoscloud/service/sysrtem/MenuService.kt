package kr.co.imoscloud.service.sysrtem

import jakarta.transaction.Transactional
import kr.co.imoscloud.core.Core
import kr.co.imoscloud.dto.MenuRequest
import kr.co.imoscloud.entity.system.Menu
import kr.co.imoscloud.entity.system.MenuRole
import kr.co.imoscloud.repository.system.MenuRepository
import kr.co.imoscloud.repository.system.MenuRoleRepository
import kr.co.imoscloud.util.AuthLevel
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service

@Service
class MenuService(
    private val core: Core,
    private val menuRepo: MenuRepository,
    private val menuRoleRepo: MenuRoleRepository,
) {

    @AuthLevel(minLevel = 5)
    fun getMenus(menuId: String?, menuName: String?): List<Menu> {
        return if (menuId==null&&menuName==null) menuRepo.findAll()
        else menuRepo.findAllByParms(menuId, "%${menuName}%")
    }

    @AuthLevel(minLevel = 5)
    @Transactional
    fun upsertMenus(req: MenuRequest): String =
        try {
            val menu: Menu = req.id
                ?.let { id ->
                    menuRepo.findByIdAndFlagActiveIsTrue(id)
                        ?.let { menu -> menu.apply {
                            menuId = req.menuId ?: this.menuId
                            upMenuId = req.upMenuId ?: this.upMenuId
                            menuName = req.menuName ?: this.menuName
                            flagSubscribe = req.flagSubscribe ?: this.flagSubscribe
                            sequence = req.sequence ?: this.sequence
                            flagActive = req.flagActive
                        }  }
                        ?: throw IllegalArgumentException("수정하려는 대상이 존재하지 않습니다. ")
                }
                ?:run {
                    val newMenu = Menu(
                        menuId = req.menuId!!,
                        upMenuId = req.upMenuId!!,
                        menuName = req.menuName!!,
                        flagSubscribe = req.flagSubscribe!!,
                        flagActive = req.flagActive,
                        sequence = req.sequence!!
                    )

                    val loginUser = SecurityUtils.getCurrentUserPrincipal()
                    val roleMap = core.getAllRoleMap(loginUser)

                    val roleIds = if (roleMap.size == 1) core.roleRepo.getAllRoleIds()
                    else roleMap.values.mapNotNull { it?.roleId }

                    val menuRoleList = roleIds.map { id ->
                        MenuRole(
                            roleId = id,
                            menuId = newMenu.menuId,
                            isOpen = req.isOpen!!,
                            isDelete = req.isDelete!!,
                            isInsert = req.isInsert!!,
                            isAdd = req.isAdd!!,
                            isPopup = req.isPopup!!,
                            isPrint = req.isPrint!!,
                            isSelect = req.isSelect!!,
                            isUpdate = req.isUpdate!!,
                        )
                    }
                    menuRoleRepo.saveAll(menuRoleList)
                    newMenu
                }

            menuRepo.save(menu)
            "${menu.menuName} 메뉴 수정 성공"
        } catch (e: NullPointerException) {
            throw IllegalArgumentException("신규 메뉴 생성 실패. : 매개변수 값을 비어있습니다. ")
        }

    @AuthLevel(minLevel = 5)
    @Transactional
    fun deleteMenu(id: Long): String {
        val menu = menuRepo.findByIdAndFlagActiveIsTrue(id)
            ?.let { menu -> menuRoleRepo.deleteAllByMenuId(menu.menuId); menu }
            ?: throw IllegalArgumentException("삭제하려는 메뉴가 존재하지 않습니다. ")

        menuRepo.delete(menu)
        return "${menu.menuName} 및 해당 메뉴에 대한 권한들 삭제 성공"
    }
}