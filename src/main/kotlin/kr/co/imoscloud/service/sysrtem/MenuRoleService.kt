package kr.co.imoscloud.service.sysrtem

import jakarta.transaction.Transactional
import kr.co.imoscloud.core.Core
import kr.co.imoscloud.dto.MenuRoleDto
import kr.co.imoscloud.entity.system.MenuRole
import kr.co.imoscloud.repository.system.MenuRoleRepository
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service

@Service
class MenuRoleService(
    private val core: Core,
    private val menuRoleRepo: MenuRoleRepository
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

    fun getInitialMenuRole(roleId: Long): List<MenuRoleDto> =
        menuRoleRepo.findAll().map { menu -> MenuRoleDto(roleId = roleId, menuId = menu.menuId) }

    @Transactional
    fun upsertMenuRole(list: List<MenuRoleDto>): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val indies = list.mapNotNull { it.id }
        val menuRoleMap = if (indies.isEmpty()) emptyMap() else menuRoleRepo.findByIdIn(indies).associateBy { it.id }

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
            }
        } catch (e: NullPointerException) {
            throw IllegalArgumentException("메뉴 권한 생성 시 필요한 정보가 부족합니다. ")
        }

        menuRoleRepo.saveAll(menuRoleList)
        return "메뉴 권한 ${upsertStr} 성공"
    }
}