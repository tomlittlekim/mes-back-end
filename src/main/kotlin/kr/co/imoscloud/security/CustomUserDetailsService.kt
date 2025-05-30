package kr.co.imoscloud.security

import kr.co.imoscloud.core.CompanyCacheManager
import kr.co.imoscloud.core.UserCacheManager
import kr.co.imoscloud.core.UserRoleCacheManager
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomUserDetailsService(
    private val ucm: UserCacheManager,
    private val rcm: UserRoleCacheManager,
    private val ccm: CompanyCacheManager
) {

    @Transactional(readOnly = true)
    fun loadUserBySiteAndUserId(site: String, userId: String): UserDetails {
        val loginUser = ucm.getUser(userId)
            ?: throw UsernameNotFoundException("로그인 유저의 정보가 메모리상에 존재하지 않습니다. ")

        val roleSummery = rcm.getUserRole(loginUser.roleId)
            ?: throw UsernameNotFoundException("권한 정보가 메모리상에 존재하지 않습니다. ")

        val companySummery = ccm.getCompany(loginUser.compCd)
        return UserPrincipal.create(loginUser, roleSummery, companySummery?.companyName)
    }
} 