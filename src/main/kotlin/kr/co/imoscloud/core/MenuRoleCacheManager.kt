package kr.co.imoscloud.core

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.system.MenuRole
import kr.co.imoscloud.repository.system.MenuRoleRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class MenuRoleCacheManager(
    val menuRoleRepo: MenuRoleRepository
) {

    companion object {
        private var menuRoleMap: MutableMap<String, Int?> = ConcurrentHashMap()
        private var upsertMap: MutableMap<String, Int?> = ConcurrentHashMap()
        private var isInspect: Boolean = false

        private fun setIsInspect(bool: Boolean) {
            isInspect = bool
        }
    }

    init {
        initialSetting()
    }

    fun getMenuRoles(roleId: Long): List<MenuRole> {
        return if (isInspect) return getAllDuringInspection(roleId)
        else {
            val index = "${roleId}-"
            menuRoleMap
                .filter { (key, value) -> key.contains(index) && value != null }
                .mapNotNull { (key, encoded) ->
                    decodePermissionBitsToMenuRole(encoded!!, roleId, key.substringAfter(index))
                }
        }
    }

    fun getMenuRole(roleId: Long, menuId: String): MenuRole? {
        return if (isInspect) getAllDuringInspection(roleId, menuId).first()
        else {
            val encoded = menuRoleMap["$roleId-$menuId"] ?: return null
            return decodePermissionBitsToMenuRole(encoded, roleId, menuId)
        }
    }

    @Transactional
    fun saveAllAndSyncCache(menuRoles: List<MenuRole>): List<MenuRole> {
        if (menuRoles.size == 1) menuRoleRepo.save(menuRoles.first())
        else menuRoleRepo.saveAll(menuRoles)

        return upsertFromMemory(menuRoles)
    }

//    @Transactional
//    fun softDeleteAndSyncCache(cs: CompanySummery, loginUserId: String): CompanySummery {
//        val result: Int = menuRoleRepo.softDeleteByCompCdAndFlagActiveIsTrue(cs.compCd, loginUserId)
//        if (result == 0) throw IllegalArgumentException("유저를 삭제하는데 실패했습니다. ")
//
//        deleteFromMemory(cs.compCd)
//        return cs
//    }

    @Scheduled(cron = "0 45 */2 * * *")
    fun inspection() {
        setIsInspect(true)
        initialSetting()
        merge()
        setIsInspect(false)
    }

    private fun initialSetting(): Unit {
        synchronized(menuRoleMap) {
            menuRoleMap.clear()
            val encodeMenuRoleMap: MutableMap<String, Int?> = mutableMapOf()

            menuRoleRepo.findAll().forEach { mr ->
                val encodeNumber = encodeMenuRolePermissions(mr)
                encodeMenuRoleMap["${mr.roleId}-${mr.menuId}"] = encodeNumber
            }
            menuRoleMap.putAll(encodeMenuRoleMap)
        }
    }

    private fun getAllDuringInspection(roleId: Long, menuId: String?=null): List<MenuRole> {
        return menuId
            ?.let { menuRoleRepo.findByRoleIdAndMenuId(roleId, menuId)?.let { listOf(it) } }
            ?:run { menuRoleRepo.findAllByRoleId(roleId) }
    }

    private fun upsertFromMemory(menuRoles: List<MenuRole>): List<MenuRole> {
        menuRoles.forEach { mr: MenuRole ->
            val index = "${mr.roleId}-${mr.menuId}"

            if (isInspect) upsertMap[index] = encodeMenuRolePermissions(mr)
            else menuRoleMap[index] = encodeMenuRolePermissions(mr)
        }

        return menuRoles
    }

    private fun deleteFromMemory(key: String): Unit {
        menuRoleMap.remove(key)
    }

    private fun merge(): Unit {
        val (firstLock, secondLock) = if (System.identityHashCode(menuRoleMap) < System.identityHashCode(upsertMap)) {
            menuRoleMap to upsertMap
        } else {
            upsertMap to menuRoleMap
        }

        synchronized(firstLock) {
            synchronized(secondLock) {
                menuRoleMap.putAll(upsertMap)
                upsertMap.clear()
            }
        }
    }

    private fun encodeMenuRolePermissions(mr: MenuRole): Int {
        val permissions = mr.toBooleanList()
        val binary = permissions.joinToString(separator = "") { booleanToTinyintStr(it) }
        return binary.toInt(2)
    }

    private fun decodePermissionBitsToMenuRole(
        encoded: Int,
        roleId: Long,
        menuId: String
    ): MenuRole {
        val bits = encoded.toString(2).padStart(9, '0').map { it == '1' }
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
            isUpdate = bits[7],
            flagCategory = bits[8]
        )
    }

    private fun booleanToTinyintStr(bool: Boolean): String = if (bool) "1" else "0"
}