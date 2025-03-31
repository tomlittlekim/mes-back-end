package kr.co.imoscloud.core

import kr.co.imoscloud.dto.RoleSummery
import kr.co.imoscloud.entity.user.User
import kr.co.imoscloud.entity.user.UserRole
import kr.co.imoscloud.iface.DtoBase
import kr.co.imoscloud.repository.company.CompanyRepository
import kr.co.imoscloud.repository.user.UserRepository
import kr.co.imoscloud.repository.user.UserRoleRepository
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractInitialSetting(
    val userRepo: UserRepository,
    val roleRepo: UserRoleRepository,
    val companyRepo: CompanyRepository,
) {
    companion object {
        private var userMap: MutableMap<Long, String?> = ConcurrentHashMap()
        private var roleMap: MutableMap<Long, RoleSummery?> = ConcurrentHashMap()
        private var companyMap: MutableMap<Long, String?> = ConcurrentHashMap()

        private var upsertUserQue: MutableMap<Long, String?> = ConcurrentHashMap()
        private var upsertRoleQue: MutableMap<Long, RoleSummery?> = ConcurrentHashMap()
        private var upsertCompanyQue: MutableMap<Long, String?> = ConcurrentHashMap()

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
                roleMap = (roleMap + upsertRoleQue) as MutableMap<Long, RoleSummery?>
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

    fun <T: DtoBase> getAllRoleMap(req: T): MutableMap<Long, RoleSummery?> {
        return if (getIsInspect()) {
            val indies = extractIDFromRequest(req)
            getAllRolesDuringInspection(indies)
        } else roleMap
    }

    protected abstract fun getAllUsersDuringInspection(indies: List<Long>): MutableMap<Long, String?>
    protected abstract fun getAllRolesDuringInspection(indies: List<Long>): MutableMap<Long, RoleSummery?>

    fun upsertUserFromInMemory(user: User) {
        if (isInspect) upsertUserQue[user.id] = user.userName
        else userMap[user.id] = user.userName
    }
    fun upsertRoleFromInMemory(userRole: UserRole) {
        if (isInspect) upsertUserQue[userRole.id] = userRole.roleName
        else userMap[userRole.id] = userRole.roleName
    }

    @Scheduled(cron = "0 0 */2 * * *")
    private fun inspection() {
        isInspect = true
        initialSettings()
    }
    
    private fun initialSettings() {
        userMap = userRepo.findAll().associate { it.id to it.userName }.toMutableMap()
        roleMap = roleRepo.findAll().associate {
            val summery = RoleSummery(it.roleName, it.priorityLevel)
            it.id to summery
        }.toMutableMap()
    }

    private fun <T: DtoBase> extractIDFromRequest(req: T): List<Long> {
        return when (req) {
            is List<*> -> req.filterIsInstance<DtoBase>().map { it.id }
            else -> listOf(req.id)
        }
    }
}