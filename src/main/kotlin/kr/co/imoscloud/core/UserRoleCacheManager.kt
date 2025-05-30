package kr.co.imoscloud.core

import jakarta.transaction.Transactional
import kr.co.imoscloud.dto.RoleSummery
import kr.co.imoscloud.entity.system.UserRole
import kr.co.imoscloud.repository.system.UserRoleRepository
import kr.co.imoscloud.security.UserPrincipal
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class UserRoleCacheManager(
    val roleRepo: UserRoleRepository
): AbstractCacheBase() {

    companion object {
        private var roleMap: MutableMap<Long, RoleSummery?> = ConcurrentHashMap()
        private var upsertMap: MutableMap<Long, RoleSummery?> = ConcurrentHashMap()
        private var isInspect: Boolean = false

        private fun setIsInspect(bool: Boolean) {
            isInspect = bool
        }
    }

    init {
        initialSetting()
    }

    final override fun initialSetting(): Unit {
        synchronized(roleMap) {
            roleMap.clear()
            roleMap.putAll(roleRepo.findAllByFlagActiveIsTrue()
                .associate { it.roleId to it.toSummery() }
                .toMutableMap())
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <K, V> getCashMap(): MutableMap<K, V?> {
        return roleMap as MutableMap<K, V?>
    }

    @Suppress("UNCHECKED_CAST")
    override fun <K, V> getUpsertMap(): MutableMap<K, V?> {
        return upsertMap as MutableMap<K, V?>
    }

    override fun isInspectEnabled(): Boolean {
        return isInspect
    }

    override fun <T, V> copyToValue(target: T?): V? {
        return if (target is RoleSummery) target.copy() as V? else null
    }

    override fun <K, V> getAllDuringInspection(indies: List<K>): MutableMap<K, V?> {
        val index = indies.filterIsInstance<Long>()

        val roleList: List<UserRole> =
            if (index.size == 1 && indies.first() != null) {
                roleRepo.findByRoleIdAndFlagActiveIsTrue(index.first()) ?.let { listOf(it) } ?: emptyList()
            } else {
                roleRepo.findAllByRoleIdInAndFlagActiveIsTrue(index)
            }

        return roleList.associate { it.roleId as K to it.toSummery() as V? }.toMutableMap()
    }

    fun getUserRoles(roleIds: List<Long>): Map<Long, RoleSummery?> {
        return getAllFromMemory(roleIds)
    }

    fun getUserRole(roleId: Long): RoleSummery? {
        return getFromMemory(roleId)
    }

    fun getRoleGroupByCompCd(loginUser: UserPrincipal): List<RoleSummery?> {
        return if (isInspect) {
            roleRepo.findAllBySearchConditionForExceptDev(loginUser.compCd).map { it.toSummery() }
        } else {
            getUserRoles(listOf(loginUser.roleId))
                .filterValues { v -> (v?.compCd == loginUser.compCd || v?.compCd == "default" ) }
                .values.toList()
                .sortedByDescending { it?.priorityLevel }
        }
    }

    @Transactional
    fun saveAllAndSyncCache(userRoles: List<UserRole>): List<UserRole> {
        return saveAllAndSyncCache(
            userRoles,
            saveFunction = { roleRepo.saveAll(it) },
            keySelector = { it.roleId },
            valueMapper = { it.toSummery() }
        )
    }

    @Transactional
    fun softDeleteAndSyncCache(rs: RoleSummery, loginUserId: String): RoleSummery {
        val result: Int = roleRepo.deleteAllByRoleId(rs.roleId, loginUserId)
        if (result == 0) throw IllegalArgumentException("유저 권한을 삭제하는데 실패했습니다. ")

        deleteByKey<Long, RoleSummery?>(rs.roleId)
        return rs
    }

    @Transactional
    fun softDeleteAllByCompCdAndSyncCache(compCd: String, loginUserId: String): Unit {
        val result: Int = roleRepo.deleteAllByCompCd(compCd, loginUserId)
        if (result == 0) return

        val map: Map<String?, List<RoleSummery?>> = buildWithNewKey<Long, RoleSummery?, String> { it?.compCd }
        val list: List<RoleSummery?>? = map[compCd]
        val roleIds: List<Long> = list?.mapNotNull { it?.roleId } ?: return
        roleIds.forEach { roleId -> deleteByKey<Long, RoleSummery?>(roleId) }
    }

    fun isDeveloper(loginUser: UserPrincipal): Boolean =
        getUserRole(loginUser.roleId)
            ?.let { it.priorityLevel == 5 }
            ?: throw IllegalArgumentException("권한 정보를 찾을 수 없습니다. ")

    fun validatePriorityIsHigherThan(roleId: Long, loginUser: UserPrincipal): Unit {
        val roleMap = getUserRoles(listOf(roleId, loginUser.roleId))

        try {
            if (isDeveloper(loginUser)) return

            val targetRole = roleMap[roleId]!!
            val loginUserRole = roleMap[loginUser.roleId]!!
            if (targetRole.priorityLevel > loginUserRole.priorityLevel) {
                val msg = "권한 레벨이 부족합니다. ${targetRole.roleName} 또는 그에 준하거나 이상의 권한이 필요합니다."
                throw IllegalArgumentException(msg)
            }
        } catch (e: NullPointerException) {
            throw IllegalArgumentException("대상: $roleId 또는 로그인 유저: ${loginUser.roleId} 의 권한 정보가 존재하지 않습니다. ")
        }
    }

    @Scheduled(cron = "0 15 */2 * * *")
    protected fun inspection() {
        setIsInspect(true)
        initialSetting()
        merge<String, RoleSummery?>()
        setIsInspect(false)
    }
}