package kr.co.imoscloud.security

import kr.co.imoscloud.core.Core
import kr.co.imoscloud.repository.user.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomUserDetailsService(
    private val userRepo: UserRepository,
    private val core: Core
) {

    @Transactional(readOnly = true)
    fun loadUserBySiteAndUserId(site: String, userId: String): UserDetails {
        val loginUserSummery = core.getUserFromInMemory(userId)
        val roleSummery = core.getUserRoleFromInMemory(loginUserSummery.roleId)
        return UserPrincipal.create(loginUserSummery, roleSummery)
    }
} 