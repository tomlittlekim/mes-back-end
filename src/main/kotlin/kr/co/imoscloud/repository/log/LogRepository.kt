package kr.co.imoscloud.repository.log

import kr.co.imoscloud.entity.log.ExceptionLog
import org.springframework.data.jpa.repository.JpaRepository


interface ExceptionLogRep:JpaRepository<ExceptionLog,Long>{}

