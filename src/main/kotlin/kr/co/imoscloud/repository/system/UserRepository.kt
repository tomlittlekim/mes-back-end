package kr.co.imoscloud.repository.system

import kr.co.imoscloud.entity.system.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.*

interface UserRepository : JpaRepository<User, Long> {
    fun findByLoginIdAndFlagActiveIsTrue(userId: String): User?

    @Query(
        """
        select u
        from User u 
        where (:loginId is not null and u.loginId = :loginId)
            and u.site = :site
    """
    )
    fun findBySiteAndLoginIdForSignUp(site: String, loginId: String?): User?
    fun findBySiteAndIdAndFlagActiveIsTrue(site: String, id: Long): User?
    fun findAllByLoginIdIn(idList: List<String?>): List<User>
    fun findByLoginId(loginId: String?): Optional<User>
    fun findAllByCompCdAndFlagActiveIsTrue(compCd: String): List<User>
    fun findByIdAndFlagActiveIsTrue(id: Long): User?
    fun findAllByFlagActiveIsTrue(): List<User>

    @Modifying
    @Query("""
        update User u
        set u.flagActive = false
        where u.compCd = :compCd
    """)
    fun deleteAllbyCompCd(compCd: String)

    @Modifying
    @Query("""
        update User u
        set 
            u.flagActive = false,
            u.updateUser = :updateUser,
            u.updateDate = :updateDate
        where u.id = :id
            and u.flagActive = true
    """)
    fun softDeleteByIdAndFlagActiveIsTrue(id: Long): Int
} 