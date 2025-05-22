package kr.co.imoscloud.security

import kr.co.imoscloud.core.Core
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomUserDetailsService(
    private val core: Core
) {

    @Transactional(readOnly = true)
    fun loadUserBySiteAndUserId(site: String, userId: String): UserDetails {
        val loginUser = core.getUserFromInMemory(userId)
            ?: throw UsernameNotFoundException("로그인 유저의 정보가 메모리상에 존재하지 않습니다. ")

        val roleSummery = core.getUserRoleFromInMemory(loginUser.roleId)
            ?: throw UsernameNotFoundException("권한 정보가 메모리상에 존재하지 않습니다. ")

        val companySummery = core.getCompanyFromInMemory(loginUser.compCd)
        return UserPrincipal.create(loginUser, roleSummery, companySummery?.companyName)
    }
} 