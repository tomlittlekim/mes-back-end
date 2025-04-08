package kr.co.imoscloud.service

import kr.co.imoscloud.core.Core
import kr.co.imoscloud.dto.RoleSummery
import kr.co.imoscloud.dto.UserRoleRequest
import kr.co.imoscloud.entity.user.MenuRole
import kr.co.imoscloud.entity.user.UserRole
import kr.co.imoscloud.util.SecurityUtils
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

    fun upsertUserRole(req: UserRoleRequest): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val roleSummery = core.getAllRoleMap(loginUser)[req.roleId]
            ?: throw IllegalArgumentException("변경하려는 권한 정보가 존재하지 않습니다. ")

        var modifyRole: UserRole = req.roleId
            ?.let { roleId ->
                core.roleRepo.findByRoleIdAndFlagActiveIsTrue(roleId)
                    ?.let { role ->
                        role.apply {
                            roleName = roleSummery.roleName
                            priorityLevel = roleSummery.priorityLevel
                            updateCommonCol(loginUser)
                        }
                    }
                    ?: throw IllegalArgumentException("권한 정보가 존재하지 않습니다. ")
            }
            ?:run {
                UserRole(
                    site = req.site ?: loginUser.getSite(),
                    compCd = req.compCd ?: loginUser.compCd,
                    roleName = roleSummery.roleName,
                    priorityLevel = roleSummery.priorityLevel
                ).apply { createCommonCol(loginUser) }
            }

        val isDev =  core.isDeveloper(loginUser)
        modifyRole = req.flagDeFault
            ?.let { flag ->
                if (isDev && modifyRole.compCd == "default" || !isDev) {
                    core.roleRepo.resetDefaultByCompCd(loginUser.compCd)
                    modifyRole.apply { flagDefault = flag }
                } else modifyRole
            }
            ?: modifyRole

        core.roleRepo.save(modifyRole)
        core.upsertFromInMemory(modifyRole)
        return "${modifyRole.roleName} 권한 생성 완료"
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
}