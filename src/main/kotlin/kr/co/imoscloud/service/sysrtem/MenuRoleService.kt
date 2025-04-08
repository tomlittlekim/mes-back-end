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
        var upsertStr: String?=null
        val modifyMenuRole: List<MenuRole> = try {
            list.map { req ->
                req.id
                    ?.let { id ->
                        upsertStr = "생성"
                        menuRoleRepo.findById(id).map { mr ->
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
                        }.orElseThrow { throw IllegalArgumentException("메뉴에 대한 권한이 존재하지 않습니다. ") }
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

        menuRoleRepo.saveAll(modifyMenuRole)
        return "메뉴 권한 ${upsertStr} 성공"
    }
}