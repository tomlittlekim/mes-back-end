package kr.co.imoscloud.service.sysrtem

import jakarta.transaction.Transactional
import kr.co.imoscloud.core.UserRoleCacheManager
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
    private val rcm: UserRoleCacheManager,
    private val menuRoleRepo: MenuRoleRepository,
) {

    fun getUserRoleSelect(): List<RoleSummery?> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        return if (rcm.isDeveloper(loginUser)) {
            rcm.getUserRoles(listOf(loginUser.roleId)).values.toList()
        } else {
            rcm.getRoleGroupByCompCd(loginUser)
        }
    }

    fun getUserRoleGroup(req: RoleSearchRequest): List<UserRole> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        return if (rcm.isDeveloper(loginUser)) {
            rcm.roleRepo.findAllBySearchConditionForDev(req.site, req.compCd, req.priorityLevel)
        } else {
            rcm.roleRepo.findAllBySearchConditionForExceptDev(loginUser.compCd, req.site, req.priorityLevel)
        }
    }

    @Transactional
    fun upsertUserRole(req: UserRoleRequest): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val changedRole: RoleSummery = rcm.getUserRole(req.fixRoleId)
            ?: throw IllegalArgumentException("적용할 권한 정보가 존재하지 않습니다. ")

        var modifyRole: UserRole = req.roleId
            ?.let { roleId ->
                rcm.roleRepo.findByRoleIdAndFlagActiveIsTrue(roleId)
                    ?.modify(changedRole, req.sequence, loginUser)
                    ?: throw IllegalArgumentException("권한 정보가 존재하지 않습니다. ")
            }
            ?:run { UserRole.create(req, changedRole.priorityLevel, loginUser) }

        val isDev =  rcm.isDeveloper(loginUser)
        modifyRole = req.flagDefault
            ?.let { flag ->
                if ((isDev && modifyRole.compCd == "default") || !isDev) {
                    rcm.roleRepo.resetDefaultByCompCd(loginUser.compCd)
                    modifyRole.apply { flagDefault = flag }
                } else modifyRole
            }
            ?: modifyRole

        rcm.saveAllAndSyncCache(listOf(modifyRole))
        return "${modifyRole.roleName} 권한 생성 완료"
    }

    @AuthLevel(minLevel = 3)
    @Transactional
    fun deleteUserRole(roleId: Long): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        return rcm.getUserRole(roleId)
            ?.let { rs: RoleSummery ->
                rcm.validatePriorityIsHigherThan(rs.roleId, loginUser)
                rcm.softDeleteAndSyncCache(rs, loginUser.loginId)
                menuRoleRepo.deleteAllByRoleId(rs.roleId)

                "${rs.roleName} 권한 삭제 및 메뉴 권한들 삭제 성공"
            }
            ?: throw IllegalArgumentException("삭제하려는 권한이 존재하지 않습니다. ")
    }
}