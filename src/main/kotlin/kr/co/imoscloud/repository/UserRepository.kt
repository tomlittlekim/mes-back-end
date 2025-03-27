package kr.co.imoscloud.repository

import kr.co.imoscloud.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserRepository : JpaRepository<User, Long> {
    fun findBySiteAndUserIdAndFlagActiveIsTrue(site: String, userId: String): User?

    @Query("""
        select u
        from User u 
        where (:userId is not null and u.userId = :userId)
            and u.site = :site
    """)
    fun findBySiteAndUserIdForSignUp(site: String, userId: String?): User?
} 