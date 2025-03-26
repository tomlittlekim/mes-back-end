package kr.co.imoscloud.entity

import kr.co.imoscloud.security.UserPrincipal
import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class CommonCol (
    @Column(name = "UPDATE_DATE")
    var upDate: LocalDateTime = LocalDateTime.now(),

    @Column(name = "UPDATE_USER_ID", length = 40)
    var upUsrId: String? = null,

    @Column(name = "CREATE_DATE")
    var inDate: LocalDateTime = LocalDateTime.now(),

    @Column(name = "CREATE_USER_ID", length = 40)
    var inUsrId: String? = null,

    @Column(name = "REMARK", length = 250)
    var remark: String? = null,

    @Column(name = "IS_ACTIVE")
    var isActive: Boolean? = true

) {
    fun createCommonCol(userPrincipal: UserPrincipal) {
        this.inDate = LocalDateTime.now()
        this.inUsrId = userPrincipal.getUserId()
        this.upDate = LocalDateTime.now()
        this.upUsrId = userPrincipal.getUserId()
    }

    fun updateCommonCol(userPrincipal: UserPrincipal) {
        this.inDate = this.inDate
        this.inUsrId = this.inUsrId
        this.upDate = LocalDateTime.now()
        this.upUsrId = userPrincipal.getUserId()
    }
}