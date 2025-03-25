package com.example.imosbackend.repository

import com.example.imosbackend.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findBySiteAndUserIdAndIsActiveIsTrue(site: String, username: String): User?
} 