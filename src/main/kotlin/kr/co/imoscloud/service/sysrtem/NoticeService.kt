package kr.co.imoscloud.service.sysrtem

import kr.co.imoscloud.repository.system.NoticeRepository
import org.springframework.stereotype.Service

@Service
class NoticeService(
    private val noticeRepo: NoticeRepository,
) {


}