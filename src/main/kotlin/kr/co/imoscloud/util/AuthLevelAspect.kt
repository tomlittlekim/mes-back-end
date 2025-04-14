package kr.co.imoscloud.util

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component

@Aspect
@Component
class AuthLevelAspect {

    @Around("@annotation(authLevel)")
    fun checkPermission(joinPoint: ProceedingJoinPoint, authLevel: AuthLevel): Any? {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val userPriority = loginUser.priorityLevel
        val requiredLevel = authLevel.minLevel

        if (userPriority < requiredLevel) {
            throw AccessDeniedException("접근 권한이 없습니다. (필요 level: $requiredLevel)")
        }

        return joinPoint.proceed()
    }
}

@Target(AnnotationTarget.FUNCTION)
annotation class AuthLevel(
    val minLevel: Int = 0
)