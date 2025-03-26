package kr.co.imoscloud.repository

import kr.co.imoscloud.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findBySiteAndUserIdAndIsActiveIsTrue(site: String, username: String): User?
} 