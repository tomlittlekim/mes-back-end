package kr.co.imoscloud.service.sysrtem

import jakarta.transaction.Transactional
import kr.co.imoscloud.core.Core
import kr.co.imoscloud.dto.MenuRoleDto
import kr.co.imoscloud.entity.system.MenuRole
import kr.co.imoscloud.util.AuthLevel
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service

@Service
class MenuRoleService(
    private val core: Core,
    private val menuService: MenuService
) {

    fun getMenuRoleGroup(roleId: Long): List<Any> {
        val menuRoleList = core.getAllMenuRoleByRoleId(roleId)

        val defaultMenuRoles = if (menuRoleList.size < menuService.getCnt()) {
            val savedMenuIds = menuRoleList.map { it.menuId }
            menuService.menuRepo.getAllByMenuIdNotIn(savedMenuIds).map {
                MenuRole(
                    roleId = roleId,
                    menuId = it.menuId,
                    flagCategory = it.upMenuId == null
                )
            }
        } else null

        val finalList = defaultMenuRoles
            ?.let { menuRoleList + defaultMenuRoles }
            ?: menuRoleList

        return finalList.map { mr ->
            MenuRoleDto(
                roleId = roleId,
                menuId = mr.menuId,
                isOpen = mr.isOpen,
                isDelete = mr.isDelete,
                isInsert = mr.isInsert,
                isAdd = mr.isAdd,
                isPopup = mr.isPopup,
                isPrint = mr.isPrint,
                isSelect = mr.isSelect,
                isUpdate = mr.isUpdate,
                flagCategory = mr.flagCategory
            )
        }
    }

    fun getMenuRole(menuId: String): MenuRole {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return core.getMenuRole(loginUser.roleId, menuId)
            ?: throw IllegalArgumentException("Menu에 대한 권한 정보가 존재하지 않습니다. ")
    }

    @AuthLevel(minLevel = 3)
    @Transactional
    fun upsertMenuRole(list: List<MenuRoleDto>): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val ids = list.mapNotNull { it.id }
        val menuRoleMap = if (ids.isEmpty()) emptyMap() else menuService.menuRoleRepo.findByIdIn(ids).associateBy { it.id }

        var upsertStr: String?=null
        val menuRoleList: List<MenuRole> = try {
            list.map { req ->
                req.roleId?.let { roleId -> core.validatePriorityIsHigherThan(roleId, loginUser) }

                menuRoleMap[req.id]
                    ?.let { mr ->
                        mr.apply {
                            roleId = req.roleId ?: this.roleId
                            menuId = req.menuId
                            isOpen = req.isOpen ?: this.isOpen
                            isDelete = req.isDelete ?: this.isDelete
                            isInsert = req.isInsert ?: this.isInsert
                            isAdd = req.isAdd ?: this.isAdd
                            isPopup = req.isPopup ?: this.isPopup
                            isPrint = req.isPrint ?: this.isPrint
                            isSelect = req.isSelect ?: this.isSelect
                            isUpdate = req.isUpdate ?: this.isUpdate
                        }
                    }
                    ?:run {
                        upsertStr = "수정"
                        MenuRole(
                            roleId = req.roleId!!,
                            menuId = req.menuId,
                            isOpen = req.isOpen  ?: false,
                            isDelete = req.isDelete ?: false,
                            isInsert = req.isInsert ?: false,
                            isAdd = req.isAdd ?: false,
                            isPopup = req.isPopup ?: false,
                            isPrint = req.isPrint ?: false,
                            isSelect = req.isSelect ?: false,
                            isUpdate = req.isUpdate ?: false,
                        )
                    }
            }
        } catch (e: NullPointerException) {
            throw IllegalArgumentException("메뉴 권한 생성 시 필요한 정보가 부족합니다. ")
        }

        menuService.menuRoleRepo.saveAll(menuRoleList)
        core.upsertMenuRoleFromInMemory(menuRoleList)
        return "메뉴 권한 ${upsertStr} 성공"
    }
}