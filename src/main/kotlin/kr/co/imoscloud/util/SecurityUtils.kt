// kr.co.imoscloud.util.SecurityUtils.kt
package kr.co.imoscloud.util

import kr.co.imoscloud.security.UserPrincipal
import org.springframework.security.core.context.SecurityContextHolder

object SecurityUtils {
    fun getCurrentUserPrincipal(): UserPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication != null && authentication.isAuthenticated && authentication.principal is UserPrincipal) {
            return authentication.principal as UserPrincipal
        }

        throw SecurityException("현재 인증된 사용자 정보를 찾을 수 없습니다.")
    }
}