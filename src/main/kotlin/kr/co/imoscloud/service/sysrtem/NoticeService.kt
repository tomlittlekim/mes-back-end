package kr.co.imoscloud.service.sysrtem

import jakarta.transaction.Transactional
import kr.co.imoscloud.dto.NoticeSearchRequest
import kr.co.imoscloud.dto.UpsertNoticeRequest
import kr.co.imoscloud.entity.system.Notice
import kr.co.imoscloud.repository.system.NoticeRepository
import kr.co.imoscloud.util.DateUtils
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.data.crossstore.ChangeSetPersister
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class NoticeService(
    private val noticeRepo: NoticeRepository,
) {

    fun getALlNotice(req: NoticeSearchRequest): List<Notice> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val fromDateTime: LocalDateTime? = req.fromDate
            ?.let { dateStr -> DateUtils.parseDate(dateStr) }
            ?.let { LocalDateTime.of(it, LocalTime.MIN) }

        val toDateTime: LocalDateTime? = req.toDate
            ?.let { dateStr -> DateUtils.parseDate(dateStr) }
            ?.let { LocalDateTime.of(it, LocalTime.MAX) }

        val (from, to) = when {
            fromDateTime != null && toDateTime == null -> Pair(fromDateTime, LocalDateTime.now().plusHours(1))
            fromDateTime == null && toDateTime != null -> Pair(LocalDateTime.of(2024,1,1,0,0,0), toDateTime)
            fromDateTime != null && toDateTime != null -> Pair(fromDateTime, toDateTime)
            else -> Pair(null, null)
        }

        return from
            ?.let { noticeRepo.findAllByCreateDateBetweenAndFlagActiveIsTrue(from, to!!) }
            ?: run { noticeRepo.findAll() }
            .filter { validatePriorityLevel(loginUser.priorityLevel, it) }
    }

    fun upsertNotice(req: UpsertNoticeRequest): String { return "" }

    fun deleteNotice(id: Long): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val target = noticeRepo.findByNoticeIdAndFlagActiveIsTrue(id)
            ?.let {
                if (!validatePriorityLevel(loginUser.priorityLevel, it))
                    throw IllegalArgumentException("권한 레벨이 부족합니다. ")

                val update = it.apply { flagActive = false; updateCommonCol(loginUser) }
                noticeRepo.save(update)
            }
            ?: throw IllegalArgumentException("삭제할 공지사항이 존재하지 않습니다. ")

        return "${target.noticeTitle} 공지사항 삭제 성공"
    }

    @Transactional
    fun upReadCountForNotice(id: Long): Unit {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        if (noticeRepo.updateReadCount(id, loginUser.priorityLevel) == 0)
            throw IllegalArgumentException("공지사항이 없거나 권한 레벨이 부족합니다. ")
    }

    private fun validatePriorityLevel(compareLevel: Int, notice: Notice): Boolean {
        return notice.priorityLevel?.let { compareLevel >= it } ?: true
    }
}