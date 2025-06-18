package kr.co.imoscloud.service.log

import kr.co.imoscloud.entity.log.ExceptionLog
import kr.co.imoscloud.repository.log.ExceptionLogRep
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class ExceptionLogService(
    val exceptionLogRep: ExceptionLogRep
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveExceptionLog(exceptionLog: ExceptionLog){
        exceptionLogRep.save(exceptionLog)
    }
}