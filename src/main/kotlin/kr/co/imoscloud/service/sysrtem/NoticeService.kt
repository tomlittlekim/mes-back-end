package kr.co.imoscloud.service.sysrtem

import jakarta.transaction.Transactional
import kr.co.imoscloud.core.Core
import kr.co.imoscloud.dto.NoticeSearchRequest
import kr.co.imoscloud.dto.UpsertNoticeRequest
import kr.co.imoscloud.entity.system.Notice
import kr.co.imoscloud.repository.system.NoticeRepository
import kr.co.imoscloud.util.AuthLevel
import kr.co.imoscloud.util.DateUtils
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class NoticeService(
    private val noticeRepo: NoticeRepository
) {

    fun getALlNotice(req: NoticeSearchRequest): List<Notice> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val (from, to) = DateUtils.getSearchDateRange(req.fromDate, req.toDate)

        return from
            ?.let { noticeRepo.findAllByCreateDateBetweenAndFlagActiveIsTrue(from, to!!) }
            ?: run { noticeRepo.findAllByFlagActiveIsTrue() }
            .filter { validatePriorityLevel(loginUser.priorityLevel, it) }
    }

    @AuthLevel(minLevel = 5)
    fun upsertNotice(req: UpsertNoticeRequest): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        var upsertStr: String? = null
        val notice: Notice = req.noticeId
            ?.let { id ->
                upsertStr = "수정"
                noticeRepo.findByNoticeIdAndFlagActiveIsTrue(id)
                    ?.let { n -> n.apply {
                        noticeTitle = req.noticeTitle ?: this.noticeTitle
                        noticeContents = req.noticeContents ?: this.noticeContents
                        noticeWriter = loginUser.loginId
                        attachmentPath = req.attachmentPath ?: this.attachmentPath
                        noticeTtl = DateUtils.parseDateTime(req.noticeTtl) ?: this.noticeTtl
                    } }
                    ?: throw IllegalArgumentException("Not found notice with id: $id")
            }
            ?:run {
                upsertStr = "생성"
                Notice(
                    noticeTitle = req.noticeTitle,
                    noticeContents = req.noticeContents,
                    noticeWriter = loginUser.loginId,
                    attachmentPath = req.attachmentPath,
                    noticeTtl = DateUtils.parseDateTime(req.noticeTtl),
                ).apply { createCommonCol(loginUser) }
            }

        noticeRepo.save(notice)
        return "${notice.noticeTitle} $upsertStr 성공"
    }

    @AuthLevel(minLevel = 5)
    fun deleteNotice(noticeId: Long): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val target = noticeRepo.findByNoticeIdAndFlagActiveIsTrue(noticeId)
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
    fun upReadCountForNotice(noticeId: Long): Unit {
        noticeRepo.updateReadCount(noticeId, SecurityUtils.getCurrentUserPrincipal().priorityLevel)
    }

    private fun validatePriorityLevel(compareLevel: Int, notice: Notice): Boolean {
        return notice.priorityLevel?.let { compareLevel >= it } ?: true
    }
}