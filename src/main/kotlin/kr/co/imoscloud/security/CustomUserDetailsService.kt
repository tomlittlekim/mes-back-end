package kr.co.imoscloud.security

import kr.co.imoscloud.repository.UserRepository
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
        val user = userRepo.findBySiteAndUserIdAndFlagActiveIsTrue(site, userId)
            ?: throw UsernameNotFoundException("User not found with username: $userId")
        
        return UserPrincipal.create(user)
    }
} 