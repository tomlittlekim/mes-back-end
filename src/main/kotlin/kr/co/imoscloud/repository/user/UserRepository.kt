package kr.co.imoscloud.repository.user

import kr.co.imoscloud.entity.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    fun findBySiteAndLoginIdAndFlagActiveIsTrue(site: String, userId: String): User?

    @Query(
        """
        select u
        from User u 
        where (:loginId is not null and u.loginId = :loginId)
            and u.site = :site
    """
    )
    fun findBySiteAndLoginIdForSignUp(site: String, loginId: String?): User?

    fun findAllByLoginIdIn(idList: List<String>): List<User>
    fun findByLoginId(loginId: String): Optional<User>
    fun findAllBySiteAndCompCdAndFlagActiveIsTrue(site: String, userId: String): List<User>
} 