package kr.co.imoscloud.repository.system

import kr.co.imoscloud.entity.system.Notice
import org.springframework.data.jpa.repository.JpaRepository

interface NoticeRepository: JpaRepository<Notice, Long> {
}