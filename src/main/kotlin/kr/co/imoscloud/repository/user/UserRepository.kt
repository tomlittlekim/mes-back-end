package kr.co.imoscloud.repository.user

import kr.co.imoscloud.entity.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

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

    fun findAllByIdIn(idList: List<Long>): List<User>
} 