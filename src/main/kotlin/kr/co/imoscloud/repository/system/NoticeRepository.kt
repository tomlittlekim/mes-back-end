package kr.co.imoscloud.repository.system

import kr.co.imoscloud.entity.system.Notice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface NoticeRepository: JpaRepository<Notice, Long> {
    fun findAllByCreateDateBetweenAndFlagActiveIsTrue(fromDate: LocalDateTime, toDate: LocalDateTime): List<Notice>
    fun findByNoticeIdAndFlagActiveIsTrue(noticeId: Long): Notice?
    fun findAllByFlagActiveIsTrue(): List<Notice>

    @Modifying
    @Query("""
        update Notice n
        set n.readCount = n.readCount + 1
        where n.noticeId = :noticeId
            and n.priorityLevel <= :priorityLevel
            and n.flagActive = true
    """)
    fun updateReadCount(noticeId: Long, priorityLevel: Int): Int
}