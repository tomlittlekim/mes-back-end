package kr.co.imoscloud.entity.log

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDateTime

@Entity
@Table(name = "EXCEPTION_LOG")
class ExceptionLog (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var id: Long? = null,

    @Column(name = "LOGGED_AT", nullable = false)
    var loggedAt: LocalDateTime? = null,

    @Column(name = "SERVICE_NAME", nullable = false, length = 100)
    var serviceName: String? = null,

    @Column(name = "METHOD_NAME", nullable = false, length = 100)
    var methodName: String? = null,

    @Column(name = "ERROR_CODE", nullable = false, length = 20)
    var errorCode: String? = null,

    @Column(name = "CAUSE_TYPE", length = 150)
    var causeType: String? = null,

    @Column(name = "CAUSE_MESSAGE", length = 500)
    var causeMessage: String? = null
)