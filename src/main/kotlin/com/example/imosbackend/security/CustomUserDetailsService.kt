package com.example.imosbackend.security

import com.example.imosbackend.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomUserDetailsService(
    private val userRepo: UserRepository
) {

    @Transactional(readOnly = true)
    fun loadUserBySiteAndUserId(site: String, userId: String): UserDetails {
        val user = userRepo.findBySiteAndUserIdAndIsActiveIsTrue(site, userId)
            ?: throw UsernameNotFoundException("User not found with username: $userId")
        
        return UserPrincipal.create(user)
    }
} 