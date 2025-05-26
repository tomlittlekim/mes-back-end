package kr.co.imoscloud.core

import jakarta.transaction.Transactional
import kr.co.imoscloud.dto.UserSummery
import kr.co.imoscloud.entity.system.User
import kr.co.imoscloud.repository.system.UserRepository
import kr.co.imoscloud.security.UserPrincipal
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class UserCacheManager(
    val userRepo: UserRepository
): AbstractCacheBase() {

    companion object {
        private var userMap: MutableMap<String, UserSummery?> = ConcurrentHashMap()
        private var upsertMap: MutableMap<String, UserSummery?> = ConcurrentHashMap()
        private var isInspect: Boolean = false

        private fun setIsInspect(bool: Boolean) {
            isInspect = bool
        }
    }

    init {
        initialSetting()
    }

    final override fun initialSetting(): Unit {
        synchronized(userMap) {
            userMap.clear()
            userMap.putAll(userRepo.findAllByFlagActiveIsTrue()
                .associate { it.loginId to User.toSummery(it) }
                .toMutableMap())
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <K, V> getCashMap(): MutableMap<K, V?> {
        return userMap as MutableMap<K, V?>
    }

    @Suppress("UNCHECKED_CAST")
    override fun <K, V> getUpsertMap(): MutableMap<K, V?> {
        return upsertMap as MutableMap<K, V?>
    }

    override fun isInspectEnabled(): Boolean {
        return isInspect
    }

    override fun <T, V> copyToValue(target: T?): V? {
        return if (target is UserSummery) target.copy() as V? else null
    }

    override fun <K, V> getAllDuringInspection(indies: List<K>): MutableMap<K, V?> {
        val index = indies.filterIsInstance<String>()

        val userList: List<User> =
            if (index.size == 1) {
                userRepo.findByLoginId(index.first()).map(::listOf)!!.orElseGet { emptyList<User>() }
            } else {
                userRepo.findAllByLoginIdIn(index)
            }

        return userList.associate { it.loginId as K to User.toSummery(it) as V? }.toMutableMap()
    }

    fun getUsers(loginIds: List<String>): Map<String, UserSummery?> {
        return getAllFromMemory(loginIds)
    }

    fun getUser(loginId: String): UserSummery? {
        return getFromMemory(loginId)
    }

    fun getAllByCompCd(loginUser: UserPrincipal): List<UserSummery?> {
        return if (isInspect) {
            userRepo.findAllByCompCdAndFlagActiveIsTrue(loginUser.compCd)
                .map { User.toSummery(it) }
        } else {
            getUsers(listOf(loginUser.loginId))
                .filterValues { it?.compCd == loginUser.compCd }
                .values.toList()
                .sortedBy { it?.id }
        }
    }

    @Transactional
    fun saveAllAndSyncCache(users: List<User>): List<User> {
        return try {
            if (users.size == 1) userRepo.save(users.first())
            else userRepo.saveAll(users)

            users.forEach { u -> upsertByEntry(u.loginId, u) }
            users
        } catch (e: Exception) {
            throw IllegalArgumentException("DB 저장 및 InMemory 동기화에 실패했습니다. ")
        }
    }

    @Transactional
    fun softDeleteAndSyncCache(us: UserSummery, loginUserId: String): UserSummery {
        val result: Int = userRepo.softDeleteByIdAndFlagActiveIsTrue(us.id, loginUserId)
        if (result == 0) throw IllegalArgumentException("유저를 삭제하는데 실패했습니다. ")

        deleteByKey<String, UserSummery?>(us.loginId)
        return us
    }

    @Scheduled(cron = "0 0 */2 * * *")
    private fun inspection() {
        setIsInspect(true)
        initialSetting()
        merge<String, UserSummery?>()
        setIsInspect(false)
    }
}