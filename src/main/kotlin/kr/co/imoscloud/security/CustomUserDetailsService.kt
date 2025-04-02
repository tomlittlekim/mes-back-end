package kr.co.imoscloud.security

import kr.co.imoscloud.core.Core
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomUserDetailsService(
    private val core: Core
) {

    @Transactional(readOnly = true)
    fun loadUserBySiteAndUserId(site: String, userId: String): UserDetails {
        val loginUser = core.getUserFromInMemory(userId)
        val roleSummery = core.getUserRoleFromInMemory(loginUser)
        return UserPrincipal.create(loginUser, roleSummery)
    }
} 