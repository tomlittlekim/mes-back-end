package kr.co.imoscloud.aop

import jakarta.persistence.PersistenceException
import kr.co.imoscloud.exception.common.GenericSaveFailedException
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Component

@Aspect
@Component
class RepositorySaveExceptionAspect {
    private val log = LoggerFactory.getLogger(RepositorySaveExceptionAspect::class.java)

    @AfterThrowing(
        pointcut = "within(kr.co.imoscloud.service..*)",
        throwing = "ex"
    )
    fun logServiceException(joinPoint: JoinPoint, ex: Throwable){
        val svcName = (joinPoint.target as Any)::class.java.simpleName
        val method  = joinPoint.signature.name
        val root    = NestedExceptionUtils.getRootCause(ex) ?: ex

        log.error(
            "[SERVICE EXCEPTION] service=$svcName, method=$method, " +
                    "cause=${root::class.simpleName}:${root.message}",
            ex
        )
    }

}
