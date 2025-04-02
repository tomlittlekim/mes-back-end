package kr.co.imoscloud.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import kr.co.imoscloud.security.UserPrincipal
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class CommonCol (
    @Column(name = "UPDATE_DATE")
    var updateDate: LocalDateTime = LocalDateTime.now(),

    @Column(name = "UPDATE_USER", length = 40)
    var updateUser: String? = null,

    @Column(name = "CREATE_DATE")
    var createDate: LocalDateTime = LocalDateTime.now(),

    @Column(name = "CREATE_USER", length = 40)
    var createUser: String? = null,

    @Column(name = "FLAG_ACTIVE")
    var flagActive: Boolean = true

) {
    fun createCommonCol(userPrincipal: UserPrincipal) {
        this.createDate = LocalDateTime.now()
        this.createUser = userPrincipal.getLoginId()
        this.updateDate = LocalDateTime.now()
        this.updateUser = userPrincipal.getLoginId()
    }

    fun updateCommonCol(userPrincipal: UserPrincipal) {
        this.createDate = this.createDate
        this.createUser = this.createUser
        this.updateDate = LocalDateTime.now()
        this.updateUser = userPrincipal.getLoginId()
    }
}