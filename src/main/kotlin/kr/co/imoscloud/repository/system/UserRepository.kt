package kr.co.imoscloud.repository.system

import kr.co.imoscloud.entity.system.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import java.util.*

interface UserRepository : JpaRepository<User, Long> {
    fun findByLoginIdAndFlagActiveIsTrue(userId: String): User?
    fun findAllByLoginIdIn(idList: List<String?>): List<User>
    fun findByLoginId(loginId: String?): Optional<User>
    fun findAllByCompCdAndFlagActiveIsTrue(compCd: String): List<User>
    fun findByIdAndFlagActiveIsTrue(id: Long): User?
    fun findAllByFlagActiveIsTrue(): List<User>
    fun findByUserNameAndPhoneNumAndFlagActiveIsTrue(userName: String, phoneNum: String): User?

    @Modifying
    @Query("""
        update User u
        set 
            u.flagActive = false,
            u.updateUser = :updateUser,
            u.updateDate = :updateDate
        where u.compCd = :compCd
            and u.flagActive is true
    """)
    fun deleteAllByCompCd(
        compCd: String,
        updateUser: String,
        updateDate: LocalDateTime ?= LocalDateTime.now()
    ): Int

    @Modifying
    @Query("""
        update User u
        set 
            u.flagActive = false,
            u.updateUser = :updateUser,
            u.updateDate = :updateDate
        where u.id = :id
            and u.flagActive = true
    """)
    fun softDeleteByIdAndFlagActiveIsTrue(
        id: Long,
        updateUser: String,
        updateDate: LocalDateTime?= LocalDateTime.now()
    ): Int
} 