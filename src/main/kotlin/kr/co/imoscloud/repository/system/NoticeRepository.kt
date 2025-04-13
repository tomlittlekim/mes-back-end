package kr.co.imoscloud.repository.system

import kr.co.imoscloud.entity.system.Notice
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface NoticeRepository: JpaRepository<Notice, Long> {
    fun findAllByCreateDateBetweenAndFlagActiveIsTrue(fromDate: LocalDateTime, toDate: LocalDateTime): List<Notice>
}