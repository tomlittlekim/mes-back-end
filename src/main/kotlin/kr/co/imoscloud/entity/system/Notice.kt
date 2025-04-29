package kr.co.imoscloud.entity.system

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "NOTICE")
@EntityListeners(AuditingEntityListener::class)
data class Notice(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOTICE_ID")
    val noticeId: Long? = null,

    @Column(name = "SITE", length = 100)
    var site: String? = null,

    @Column(name = "COMP_CD", length = 100)
    var compCd: String? = null,

    @Column(name = "NOTICE_TITLE", length = 100)
    var noticeTitle: String? = null,

    @Column(name = "ATTACHMENT_PATH", length = 200)
    var attachmentPath: String? = null,

    @Column(name = "NOTICE_CONTENTS")
    var noticeContents: String? = null,

    @Column(name = "NOTICE_WRITER", length = 100)
    var noticeWriter: String? = null,

    @Column(name = "READ_COUNT")
    var readCount: Int? = 0,

    @Column(name = "PRIORITY_LEVEL")
    var priorityLevel: Int? = 0,

    @Column(name = "NOTICE_TTL")
    var noticeTtl: LocalDateTime? = null

) : CommonCol()