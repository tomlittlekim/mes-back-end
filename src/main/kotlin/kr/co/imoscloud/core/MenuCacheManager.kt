package kr.co.imoscloud.core

import jakarta.transaction.Transactional
import kr.co.imoscloud.dto.CompanySummery
import kr.co.imoscloud.entity.system.Menu
import kr.co.imoscloud.repository.system.MenuRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class MenuCacheManager(
    val menuRepo: MenuRepository,
): AbstractCacheBase() {

    companion object {
        private var menuMap: MutableMap<String, Menu?> = ConcurrentHashMap()
        private var upsertMap: MutableMap<String, Menu?> = ConcurrentHashMap()
        private var isInspect: Boolean = false

        private fun setIsInspect(bool: Boolean) {
            isInspect = bool
        }
    }

    init {
        initialSetting()
    }

    final override fun initialSetting(): Unit {
        synchronized(menuMap) {
            menuMap.clear()
            menuMap.putAll(menuRepo.findAllByFlagActiveIsTrue()
                .associateBy { it.menuId }
                .toMutableMap())
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <K, V> getCashMap(): MutableMap<K, V?> {
        return menuMap as MutableMap<K, V?>
    }

    @Suppress("UNCHECKED_CAST")
    override fun <K, V> getUpsertMap(): MutableMap<K, V?> {
        return upsertMap as MutableMap<K, V?>
    }

    override fun isInspectEnabled(): Boolean {
        return isInspect
    }

    override fun <T, V> copyToValue(target: T?): V? {
        return if (target is Menu) target.copy() as V? else null
    }

    override fun <K, V> getAllDuringInspection(indies: List<K>): MutableMap<K, V?> {
        return menuRepo.findAllByFlagActiveIsTrue()
            .associate { it.menuId as K to it.copy() as V? }
            .toMutableMap()
    }

    // param 상관 없음 대신 String 값 아무거나 넣어주면 됨
    fun getMenus(menuId: List<String>): Map<String, Menu?> {
        return getAllFromMemory(menuId)
    }

    fun getMenu(menuId: String): Menu? {
        return getMenus(listOf("string"))[menuId]
    }

    @Transactional
    fun saveAllAndSyncCache(companies: List<Menu>): List<Menu> {
        return saveAllAndSyncCache(
            companies,
            saveFunction = { menuRepo.saveAll(it) },
            keySelector = { it.menuId },
            valueMapper = { it }
        )
    }

    @Transactional
    fun deleteAndSyncCache(menu: Menu): Menu {
        menuRepo.delete(menu)
        deleteByKey<String, Menu?>(menu.menuId)
        return menu
    }

    @Scheduled(cron = "0 59 2 */2 * *")
    fun inspection() {
        setIsInspect(true)
        initialSetting()
        merge<String, CompanySummery?>()
        setIsInspect(false)
    }
}