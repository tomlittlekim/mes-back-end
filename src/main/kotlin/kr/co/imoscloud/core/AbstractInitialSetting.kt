package kr.co.imoscloud.core

import kr.co.imoscloud.dto.CompanySummery
import kr.co.imoscloud.dto.RoleSummery
import kr.co.imoscloud.dto.UserSummery
import kr.co.imoscloud.entity.company.Company
import kr.co.imoscloud.entity.user.MenuRole
import kr.co.imoscloud.entity.user.User
import kr.co.imoscloud.entity.user.UserRole
import kr.co.imoscloud.iface.DtoCompCdBase
import kr.co.imoscloud.iface.DtoLoginIdBase
import kr.co.imoscloud.iface.DtoRoleIdBase
import kr.co.imoscloud.repository.company.CompanyRepository
import kr.co.imoscloud.repository.user.MenuRoleRepository
import kr.co.imoscloud.repository.user.UserRepository
import kr.co.imoscloud.repository.user.UserRoleRepository
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractInitialSetting(
    val userRepo: UserRepository,
    val roleRepo: UserRoleRepository,
    val companyRepo: CompanyRepository,
    val menuRoleRepo: MenuRoleRepository
) {
    companion object {
        private var userMap: MutableMap<String, UserSummery?> = ConcurrentHashMap()
        private var roleMap: MutableMap<Long, RoleSummery?> = ConcurrentHashMap()
        private var companyMap: MutableMap<String, CompanySummery?> = ConcurrentHashMap()
        private var menuRoleMap: MutableMap<String, Int?> = ConcurrentHashMap()

        private var upsertUserQue: MutableMap<String, UserSummery?> = ConcurrentHashMap()
        private var upsertRoleQue: MutableMap<Long, RoleSummery?> = ConcurrentHashMap()
        private var upsertCompanyQue: MutableMap<String, CompanySummery?> = ConcurrentHashMap()
        private var upsertMenuRoleQue: MutableMap<String, Int?> = ConcurrentHashMap()

        private var isInspect: Boolean = false
            set(value) {
                field = value
                if (!value) upsertForSelectMap()
            }

        private fun upsertForSelectMap() {
            synchronized(upsertUserQue) {
                userMap = (userMap + upsertUserQue) as MutableMap<String, UserSummery?>
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
            synchronized(upsertMenuRoleQue) {
                menuRoleMap = (menuRoleMap + upsertMenuRoleQue) as MutableMap<String, Int?>
                upsertMenuRoleQue = ConcurrentHashMap()
            }
        }

        fun getIsInspect(): Boolean = isInspect
    }
    
    init {
        initialSettings()
    }

    fun <T: DtoLoginIdBase> getAllUserMap(req: List<T?>): MutableMap<String, UserSummery?> {
        return if (getIsInspect()) {
            val indies = extractUserIdFromRequest(req)
            getAllUsersDuringInspection(indies)
        } else userMap
    }
    fun getAllUserMapByIndies(indies: List<String>): MutableMap<String, UserSummery?> {
        return if (getIsInspect()) getAllUsersDuringInspection(indies)
        else userMap
    }

    fun <T: DtoRoleIdBase>  getAllRoleMap(req: List<T?>): MutableMap<Long, RoleSummery?> {
        return if (getIsInspect()) {
            val indies = extractRoleIdFromRequest(req)
            getAllRolesDuringInspection(indies)
        } else roleMap
    }
    fun getAllRoleMapByIndies(indies: List<Long>): MutableMap<Long, RoleSummery?> {
        return if (getIsInspect()) getAllRolesDuringInspection(indies)
        else roleMap
    }

    fun <T: DtoCompCdBase>  getAllCompanyMap(req: List<T?>): MutableMap<String, CompanySummery?> {
        return if (getIsInspect()) {
            val indies = extractCompCdFromRequest(req)
            getAllCompanyDuringInspection(indies)
        } else companyMap
    }
    fun getAllCompanyMapByIndies(indies: List<String>): MutableMap<String, CompanySummery?> {
        return if (getIsInspect()) getAllCompanyDuringInspection(indies)
        else companyMap
    }

    fun getMenuRole(roleId: Long, menuId: String): MenuRole? {
        return if (getIsInspect()) getMenuRoleDuringInspection(roleId, menuId)
        else {
            val encoded = menuRoleMap["$roleId-$menuId"] ?: return null
            val bits = encoded.toString(2).padStart(8, '0').map { it == '1' }
            return MenuRole(
                id = -1L,
                roleId = roleId,
                menuId = menuId,
                isOpen = bits[0],
                isDelete = bits[1],
                isInsert = bits[2],
                isAdd = bits[3],
                isPopup = bits[4],
                isPrint = bits[5],
                isSelect = bits[6],
                isUpdate = bits[7]
            )
        }
    }

    protected abstract fun getAllUsersDuringInspection(indies: List<String?>): MutableMap<String, UserSummery?>
    protected abstract fun getAllRolesDuringInspection(indies: List<Long?>): MutableMap<Long, RoleSummery?>
    protected abstract fun getAllCompanyDuringInspection(indies: List<String?>): MutableMap<String, CompanySummery?>
    protected abstract fun getMenuRoleDuringInspection(roleId: Long, menuId: String): MenuRole?

    fun upsertUserFromInMemory(user: User) {
        val summery = userToSummery(user)
        if (isInspect) upsertUserQue[user.loginId] = summery
        else userMap[user.loginId] = summery
    }
    fun upsertRoleFromInMemory(userRole: UserRole) {
        val summery = roleToSummery(userRole)
        if (isInspect) upsertRoleQue[userRole.roleId] = summery
        else roleMap[userRole.roleId] = summery
    }
    fun upsertCompanyFromInMemory(company: Company) {
        val summery = CompanySummery(company.id, company.companyName)
        if (isInspect) upsertCompanyQue[company.compCd] = summery
        else companyMap[company.compCd] = summery
    }
    fun upsertMenuRoleFromInMemory(mr: MenuRole) {
        val index = "${mr.roleId}-${mr.menuId}"
        val encoded = encodeMenuRolePermissions(mr)
        if (isInspect) upsertMenuRoleQue[index] = encoded
        else menuRoleMap[index] = encoded
    }

    fun <T> extractAllFromRequest(req: List<T>): Map<String, List<Any>> {
        val userIdList = mutableListOf<String>()
        val roleIdList = mutableListOf<Long>()
        val companyIdList = mutableListOf<String>()

        req.forEach { item ->
            if (item is DtoLoginIdBase) userIdList.add(item.loginId)
            if (item is DtoRoleIdBase) roleIdList.add(item.roleId)
            if (item is DtoCompCdBase) companyIdList.add(item.compCd)
        }

        return mapOf(
            "userIdList" to userIdList,
            "roleIdList" to roleIdList,
            "companyIdList" to companyIdList
        )
    }

    fun roleToSummery(it: UserRole): RoleSummery = RoleSummery(it.roleName, it.priorityLevel)
    fun userToSummery(u: User): UserSummery = UserSummery(
        u.id,u.site,u.compCd,u.userName,u.loginId,u.userPwd,u.imagePath,u.roleId,u.userEmail,u.phoneNum,u.departmentId,u.positionId,u.flagActive
    )

    @Scheduled(cron = "0 0 */2 * * *")
    private fun inspection() {
        isInspect = true
        initialSettings()
    }
    
    private fun initialSettings() {
        userMap = userRepo.findAll().associate {
            val summery = userToSummery(it)
            it.loginId to summery
        }.toMutableMap()

        roleMap = roleRepo.findAll().associate { it.roleId to roleToSummery(it) }.toMutableMap()

        companyMap = companyRepo.findAll().associate {
            val summery = CompanySummery(it.id, it.companyName)
            it.compCd to summery
        }.toMutableMap()

        val encodeMenuRoleMap: MutableMap<String, Int?> = mutableMapOf()
        menuRoleRepo.findAll().forEach { mr ->
            val encodeNumber = encodeMenuRolePermissions(mr)
            encodeMenuRoleMap["${mr.roleId}-${mr.menuId}"] = encodeNumber
        }
        menuRoleMap = encodeMenuRoleMap
    }

    private fun <T: DtoLoginIdBase> extractUserIdFromRequest(req: List<T?>): List<String?> {
        if (req.isEmpty()) throw IllegalArgumentException("Request is empty")
        return req.map { it?.loginId }
    }

    private fun <T: DtoRoleIdBase> extractRoleIdFromRequest(req: List<T?>): List<Long?> {
        if (req.isEmpty()) throw IllegalArgumentException("Request is empty")
        return req.map { it?.roleId }
    }

    private fun <T: DtoCompCdBase> extractCompCdFromRequest(req: List<T?>): List<String?> {
        if (req.isEmpty()) throw IllegalArgumentException("Request is empty")
        return req.map { it?.compCd }
    }

    private fun encodeMenuRolePermissions(mr: MenuRole): Int {
        val permissions = listOf(mr.isOpen, mr.isDelete, mr.isInsert, mr.isAdd, mr.isPopup, mr.isPrint, mr.isSelect, mr.isUpdate)
        val binary = permissions.joinToString(separator = "") { booleanToTinyintStr(it) }
        return binary.toInt(2)
    }

    private fun booleanToTinyintStr(bool: Boolean): String = if (bool) "1" else "0"
}