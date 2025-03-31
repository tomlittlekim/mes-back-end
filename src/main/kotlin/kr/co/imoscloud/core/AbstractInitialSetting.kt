package kr.co.imoscloud.core

import kr.co.imoscloud.dto.CompanySummery
import kr.co.imoscloud.dto.RoleSummery
import kr.co.imoscloud.entity.company.Company
import kr.co.imoscloud.entity.user.User
import kr.co.imoscloud.entity.user.UserRole
import kr.co.imoscloud.iface.*
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
        private var companyMap: MutableMap<String, CompanySummery?> = ConcurrentHashMap()
        private var groupMap: MutableMap<Long, Int?> = ConcurrentHashMap()

        private var upsertUserQue: MutableMap<Long, String?> = ConcurrentHashMap()
        private var upsertRoleQue: MutableMap<Long, RoleSummery?> = ConcurrentHashMap()
        private var upsertCompanyQue: MutableMap<String, CompanySummery?> = ConcurrentHashMap()
        private var upsertGroupQue: MutableMap<Long, Int?> = ConcurrentHashMap()

        private var isInspect: Boolean = true
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
            synchronized(upsertCompanyQue) {
                companyMap = (companyMap + upsertCompanyQue) as MutableMap<String, CompanySummery?>
                upsertCompanyQue = ConcurrentHashMap()
            }
            synchronized(upsertGroupQue) {
                groupMap = (groupMap + upsertGroupQue) as MutableMap<Long, Int?>
                upsertGroupQue = ConcurrentHashMap()
            }
        }

        fun getIsInspect(): Boolean = isInspect
    }
    
    init {
        initialSettings()
    }

    fun <T: DtoUserIdBase> getAllUserMap(req: List<T>): MutableMap<Long, String?> {
        return if (getIsInspect()) {
            val indies = extractIdFromRequest(req)
            getAllUsersDuringInspection(indies)
        } else userMap
    }

    fun <T: DtoRoleIdBase>  getAllRoleMap(req: List<T>): MutableMap<Long, RoleSummery?> {
        return if (getIsInspect()) {
            val indies = extractIdFromRequest(req)
            getAllRolesDuringInspection(indies)
        } else roleMap
    }

    fun <T: DtoCompCdBase>  getAllCompanyMap(req: List<T>): MutableMap<String, CompanySummery?> {
        return if (getIsInspect()) {
            val indies = extractIdFromRequest(req)
            getAllCompanyDuringInspection(indies)
        } else companyMap
    }

    protected abstract fun getAllUsersDuringInspection(indies: List<Long>): MutableMap<Long, String?>
    protected abstract fun getAllRolesDuringInspection(indies: List<Long>): MutableMap<Long, RoleSummery?>
    protected abstract fun getAllCompanyDuringInspection(indies: List<String>): MutableMap<String, CompanySummery?>

    fun upsertUserFromInMemory(user: User) {
        if (isInspect) upsertUserQue[user.id] = user.userName
        else userMap[user.id] = user.userName
    }
    fun upsertRoleFromInMemory(userRole: UserRole) {
        val summery = RoleSummery(userRole.roleName, userRole.priorityLevel)
        if (isInspect) upsertRoleQue[userRole.id] = summery
        else roleMap[userRole.id] = summery
    }
    fun upsertCompanyFromInMemory(company: Company) {
        val summery = CompanySummery(company.id, company.companyName)
        if (isInspect) upsertCompanyQue[company.compCd] = summery
        else companyMap[company.compCd] = summery
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
        companyMap = companyRepo.findAll().associate {
            val summery = CompanySummery(it.id, it.companyName)
            it.compCd to summery
        }.toMutableMap()
        groupMap
    }

    private fun <T: DtoUserIdBase> extractIdFromRequest(req: List<T>): List<Long> {
        if (req.isEmpty()) throw IllegalArgumentException("Request is empty")
        return req.map { it.userId }
    }

    private fun <T: DtoRoleIdBase> extractIdFromRequest(req: List<T>): List<Long> {
        if (req.isEmpty()) throw IllegalArgumentException("Request is empty")
        return req.map { it.roleId }
    }

    private fun <T: DtoCompCdBase> extractIdFromRequest(req: List<T>): List<String> {
        if (req.isEmpty()) throw IllegalArgumentException("Request is empty")
        return req.map { it.compCd }
    }

    private fun <T> extractIdFromRequest(req: List<T>): Map<String, List<Any>> {
        val userIdList = mutableListOf<Long>()
        val roleIdList = mutableListOf<Long>()
        val companyIdList = mutableListOf<String>()

        req.forEach { item ->
            if (item is DtoUserIdBase) userIdList.add(item.userId)
            if (item is DtoRoleIdBase) roleIdList.add(item.roleId)
            if (item is DtoCompCdBase) companyIdList.add(item.compCd)
        }

        return mapOf(
            "userIdList" to userIdList,
            "roleIdList" to roleIdList,
            "companyIdList" to companyIdList
        )
    }
}