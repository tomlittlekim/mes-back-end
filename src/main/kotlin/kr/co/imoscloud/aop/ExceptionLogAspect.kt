package kr.co.imoscloud.aop

import kr.co.imoscloud.entity.log.ExceptionLog
import kr.co.imoscloud.exception.ErrorCode
import kr.co.imoscloud.exception.ImosException
import kr.co.imoscloud.service.log.ExceptionLogService
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Aspect
@Component
class ExceptionLogAspect(
    private val exceptionLogService: ExceptionLogService
) {
    private val log = LoggerFactory.getLogger(ExceptionLogAspect::class.java)

    @AfterThrowing(
        pointcut = "within(kr.co.imoscloud.service..*) && !within(kr.co.imoscloud.service.log.ExceptionLogService)",
        throwing = "ex"
    )
    fun logServiceException(joinPoint: JoinPoint, ex: Throwable){
        val svcName = (joinPoint.target as Any)::class.java.simpleName
        val method  = joinPoint.signature.name
        val root    = NestedExceptionUtils.getRootCause(ex) ?: ex

        val code = when (ex) {
            is ImosException -> ex.errorCode.code
            is DataIntegrityViolationException -> ErrorCode.GENERIC_SAVE_FAILED.code
            else -> "UNKNOWN"
        }

        val entry = ExceptionLog(
            loggedAt     = LocalDateTime.now(),
            serviceName  = svcName,
            methodName   = method,
            errorCode    = code,
            causeType    = root::class.simpleName,
            causeMessage = root.message
        )

        try {
            exceptionLogService.saveExceptionLog(entry)
        } catch (e: Exception) {
            log.warn("ExceptionLog 저장 실패: ${e.message}", e)
        }

        log.error(
            "[SERVICE EXCEPTION] service=$svcName, method=$method, " +
                    "cause=${root::class.simpleName}:${root.message}",
            ex
        )
    }

}
