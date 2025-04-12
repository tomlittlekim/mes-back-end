package kr.co.imoscloud.service.sysrtem

import jakarta.transaction.Transactional
import kr.co.imoscloud.core.Core
import kr.co.imoscloud.dto.RoleSearchRequest
import kr.co.imoscloud.dto.RoleSummery
import kr.co.imoscloud.dto.UserRoleRequest
import kr.co.imoscloud.entity.system.UserRole
import kr.co.imoscloud.repository.system.MenuRoleRepository
import kr.co.imoscloud.util.AuthLevel
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service

@Service
class UserRoleService(
    private val core: Core,
    private val menuRoleRepo: MenuRoleRepository,
) {

    fun getUserRoleSelect(): List<RoleSummery?> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        return if (core.isDeveloper(loginUser)) {
            core.getAllRoleMap(loginUser).values.toList()
        } else {
            core.getRoleGroupByCompCd(loginUser)
        }
    }

    fun getUserRoleGroup(req: RoleSearchRequest): List<UserRole> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        return if (core.isDeveloper(loginUser)) {
            core.roleRepo.findAllBySearchConditionForDev(req.site, req.compCd, req.priorityLevel)
        } else {
            core.roleRepo.findAllBySearchConditionForExceptDev(loginUser.compCd, req.site, req.priorityLevel)
        }
    }

    @Transactional
    fun upsertUserRole(req: UserRoleRequest): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val roleSummery = core.getAllRoleMap(loginUser)[req.fixRoleId]
            ?: throw IllegalArgumentException("적용할 권한 정보가 존재하지 않습니다. ")

        var modifyRole: UserRole = req.roleId
            ?.let { roleId ->
                core.roleRepo.findByRoleIdAndFlagActiveIsTrue(roleId)
                    ?.let { role ->
                        role.apply {
                            roleName = roleSummery.roleName
                            priorityLevel = roleSummery.priorityLevel
                            sequence = req.sequence ?: this.sequence
                            updateCommonCol(loginUser)
                        }
                    }
                    ?: throw IllegalArgumentException("권한 정보가 존재하지 않습니다. ")
            }
            ?:run {
                UserRole(
                    site = req.site ?: loginUser.getSite(),
                    compCd = req.compCd ?: loginUser.compCd,
                    roleName = req.roleName!!,
                    priorityLevel = roleSummery.priorityLevel,
                    sequence = req.sequence
                ).apply { createCommonCol(loginUser) }
            }

        val isDev =  core.isDeveloper(loginUser)
        modifyRole = req.flagDefault
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

    @AuthLevel(minLevel = 3)
    @Transactional
    fun deleteUserRole(roleId: Long): String {
        val role = core.roleRepo.findByRoleIdAndFlagActiveIsTrue(roleId)
            ?.let { role ->
                core.validatePriorityIsHigherThan(role.roleId, SecurityUtils.getCurrentUserPrincipal())
                menuRoleRepo.deleteAllByRoleId(role.roleId)
                role
            }
            ?: throw IllegalArgumentException("삭제하려는 권한이 존재하지 않습니다. ")

        core.roleRepo.delete(role)
        return "${role.roleName} 권한 삭제 및 메뉴 권한들 삭제 성공"
    }
}