package kr.co.imoscloud.util

import kr.co.imoscloud.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

object SecurityUtils {
    private val logger = LoggerFactory.getLogger(SecurityUtils::class.java)

    /**
     * 현재 사용자의 인증 정보를 가져옵니다.
     * 인증된 사용자가 없거나 익명 인증인 경우 예외를 발생시킵니다.
     */
    fun getCurrentUserPrincipal(): UserPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication

        // 디버깅 정보 로그
        logger.debug("인증 정보 확인: {}", authentication != null)
        if (authentication != null) {
            logger.debug("인증 객체 타입: {}", authentication.javaClass.name)
            logger.debug("인증 여부: {}", authentication.isAuthenticated)

            // principal 객체 정보 확인
            val principal = authentication.principal
            logger.debug("Principal 타입: {}", principal?.javaClass?.name ?: "null")

            // principal 객체가 UserPrincipal 타입인지 확인
            if (principal is UserPrincipal) {
                logger.debug("UserPrincipal ID: {}, 사용자명: {}", principal.getId(), principal.getUsername())
            } else {
                logger.debug("Principal은 UserPrincipal 타입이 아님")
            }
        }

        // 1. 인증 객체가 존재하는지 확인
        if (authentication == null) {
            logger.warn("인증 정보가 없음")
            throw SecurityException("현재 인증된 사용자 정보를 찾을 수 없습니다.")
        }

        // 2. 익명 인증인지 확인
        if (authentication is AnonymousAuthenticationToken) {
            logger.warn("익명 인증: {}", authentication.principal)
            throw SecurityException("현재 인증된 사용자 정보를 찾을 수 없습니다.")
        }

        // 3. 인증되었는지 확인
        if (!authentication.isAuthenticated) {
            logger.warn("인증되지 않은 사용자")
            throw SecurityException("현재 인증된 사용자 정보를 찾을 수 없습니다.")
        }

        // 4. UserPrincipal 타입인지 확인
        if (authentication.principal !is UserPrincipal) {
            logger.warn("Principal이 UserPrincipal 타입이 아님: {}",
                authentication.principal?.javaClass?.name ?: "null")
            throw SecurityException("현재 인증된 사용자 정보를 찾을 수 없습니다.")
        }

        // 모든 검증을 통과한 경우 UserPrincipal 반환
        return authentication.principal as UserPrincipal
    }

    /**
     * 현재 사용자의 인증 정보를 안전하게 가져옵니다.
     * 인증된 사용자가 없거나 익명 인증인 경우 null을 반환합니다.
     */
    fun getCurrentUserPrincipalOrNull(): UserPrincipal? {
        try {
            return getCurrentUserPrincipal()
        } catch (e: SecurityException) {
            logger.warn("사용자 인증 정보를 찾을 수 없습니다: {}", e.message)
            return null
        }
    }
}