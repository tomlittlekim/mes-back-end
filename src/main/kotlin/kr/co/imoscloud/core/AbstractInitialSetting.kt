package kr.co.imoscloud.core

import kr.co.imoscloud.entity.user.User
import kr.co.imoscloud.entity.user.UserRole
import kr.co.imoscloud.iface.DtoBase
import kr.co.imoscloud.repository.user.UserRepository
import kr.co.imoscloud.repository.user.UserRoleRepository
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractInitialSetting(
    val userRepo: UserRepository,
    val roleRepo: UserRoleRepository
) {
    companion object {
        private var userMap: MutableMap<Long, String?> = ConcurrentHashMap()
        private var roleMap: MutableMap<Long, String?> = ConcurrentHashMap()
        private var upsertUserQue: MutableMap<Long, String?> = ConcurrentHashMap()
        private var upsertRoleQue: MutableMap<Long, String?> = ConcurrentHashMap()

        private var isInspect: Boolean = false
            set(value) {
                field = value
                if (!value) upsertForSelectMap()
            }

        private fun upsertForSelectMap() {
            synchronized(upsertUserQue) {
                userMap = (userMap + upsertUserQue) as MutableMap<Long, String?>
                upsertUserQue = ConcurrentHashMap()
            }
            synchronized(upsertRoleQue) {
                roleMap = (roleMap + upsertRoleQue) as MutableMap<Long, String?>
                upsertRoleQue = ConcurrentHashMap()
            }
        }

        fun getIsInspect(): Boolean = isInspect
    }
    
    init {
        initialSettings()
    }

    fun <T: DtoBase> getAllUserMap(req: T): MutableMap<Long, String?> {
        return if (getIsInspect()) {
            val indies = extractIDFromRequest(req)
            getAllUsersDuringInspection(indies)
        } else userMap
    }

    fun <T: DtoBase> getAllRoleMap(req: T): MutableMap<Long, String?> {
        return if (getIsInspect()) {
            val indies = extractIDFromRequest(req)
            getAllRolesDuringInspection(indies)
        } else roleMap
    }

    protected abstract fun getAllUsersDuringInspection(indies: List<Long>): MutableMap<Long, String?>
    protected abstract fun getAllRolesDuringInspection(indies: List<Long>): MutableMap<Long, String?>

    fun addUser(user: User) {}
    fun modifyUser(targetId: Long, user: User) {}
    fun addRole(userRole: UserRole) {}
    fun modifyRole(targetId: Long, userRole: UserRole) {}

    @Scheduled(cron = "0 0 */2 * * *")
    private fun inspection() {
        isInspect = true
        initialSettings()
    }
    
    private fun initialSettings() {
        userMap = userRepo.findAll().associate { it.id to it.userName }.toMutableMap()
        roleMap = roleRepo.findAll().associate { it.id to it.roleName }.toMutableMap()
    }

    private fun <T: DtoBase> extractIDFromRequest(req: T): List<Long> {
        return when (req) {
            is List<*> -> req.filterIsInstance<DtoBase>().map { it.id }
            else -> listOf(req.id)
        }
    }
}