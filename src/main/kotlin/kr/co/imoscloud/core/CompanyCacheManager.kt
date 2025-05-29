package kr.co.imoscloud.core

import jakarta.transaction.Transactional
import kr.co.imoscloud.dto.CompanySummery
import kr.co.imoscloud.entity.system.Company
import kr.co.imoscloud.repository.system.CompanyRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class CompanyCacheManager(
    val companyRepo: CompanyRepository
): AbstractCacheBase() {

    companion object {
        private var companyMap: MutableMap<String, CompanySummery?> = ConcurrentHashMap()
        private var upsertMap: MutableMap<String, CompanySummery?> = ConcurrentHashMap()
        private var isInspect: Boolean = false

        private fun setIsInspect(bool: Boolean) {
            isInspect = bool
        }
    }

    init {
        initialSetting()
    }

    final override fun initialSetting(): Unit {
        synchronized(companyMap) {
            companyMap.clear()
            companyMap.putAll(companyRepo.findAllByFlagActiveIsTrue()
                .associate { it.compCd to Company.toSummery(it) }
                .toMutableMap())
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <K, V> getCashMap(): MutableMap<K, V?> {
        return companyMap as MutableMap<K, V?>
    }

    @Suppress("UNCHECKED_CAST")
    override fun <K, V> getUpsertMap(): MutableMap<K, V?> {
        return upsertMap as MutableMap<K, V?>
    }

    override fun isInspectEnabled(): Boolean {
        return isInspect
    }

    override fun <T, V> copyToValue(target: T?): V? {
        return if (target is CompanySummery) target.copy() as V? else null
    }

    override fun <K, V> getAllDuringInspection(indies: List<K>): MutableMap<K, V?> {
        val index = indies.filterIsInstance<String>()

        val userList: List<Company> =
            if (index.size == 1) {
                companyRepo.findByCompCd(index.first()).map(::listOf)!!.orElseGet { emptyList<Company>() }
            } else {
                companyRepo.findAllByCompCdIn(index)
            }

        return userList.associate { it.compCd as K to Company.toSummery(it) as V? }.toMutableMap()
    }

    fun getCompanies(compCds: List<String>): Map<String, CompanySummery?> {
        return getAllFromMemory(compCds)
    }

    fun getCompany(compCd: String): CompanySummery? {
        return getFromMemory(compCd)
    }

    @Transactional
    fun saveAllAndSyncCache(companies: List<Company>): List<Company> {
        return saveAllAndSyncCache(
            companies,
            saveFunction = { companyRepo.saveAll(it) },
            keySelector = { it.compCd },
            valueMapper = { Company.toSummery(it) }
        )
    }

    @Transactional
    fun softDeleteAndSyncCache(cs: CompanySummery, loginUserId: String): CompanySummery {
        val result: Int = companyRepo.softDeleteByCompCdAndFlagActiveIsTrue(cs.compCd, loginUserId)
        if (result == 0) throw IllegalArgumentException("유저를 삭제하는데 실패했습니다. ")

        deleteByKey<String, CompanySummery?>(cs.compCd)
        return cs
    }

    @Scheduled(cron = "0 55 */2 * * *")
    fun inspection() {
        setIsInspect(true)
        initialSetting()
        merge<String, CompanySummery?>()
        setIsInspect(false)
    }
}