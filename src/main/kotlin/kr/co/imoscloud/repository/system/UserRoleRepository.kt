package kr.co.imoscloud.repository.system

import kr.co.imoscloud.entity.system.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface UserRoleRepository: JpaRepository<UserRole, Long> {
    fun findAllByRoleIdInAndFlagActiveIsTrue(idList: List<Long?>): List<UserRole>
    fun findAllByFlagActiveIsTrue(): List<UserRole>

    @Query("""
        select ur
        from UserRole ur
        where (:site is null or ur.site = :site)
            and (ur.compCd = 'default' or ur.compCd = :compCd)
            and (:priorityLevel is null or ur.priorityLevel = :priorityLevel)
            and ur.flagActive is true 
        order by ur.sequence desc
    """)
    fun findAllBySearchConditionForExceptDev(compCd: String, site: String?=null, priorityLevel: Int?=null): List<UserRole>

    @Query("""
        select ur
        from UserRole ur
        where (:site is null or ur.site = :site)
            and (:compCd is null or ur.compCd = :compCd)
            and (:priorityLevel is null or ur.priorityLevel = :priorityLevel)
            and ur.flagActive is true
        order by ur.sequence desc
    """)
    fun findAllBySearchConditionForDev(site: String?=null, compCd: String?=null, priorityLevel: Int?=null): List<UserRole>

    fun findByRoleIdAndFlagActiveIsTrue(roleId: Long): UserRole?

    @Modifying
    @Query("""
        update UserRole ur
        set ur.flagDefault = false 
        where ur.compCd = :compCd
            and ur.flagActive is true
            and ur.flagDefault is true
    """)
    fun resetDefaultByCompCd(compCd: String)

    @Query("""
        select ur.roleId
        from UserRole ur
        where ur.flagActive is true
    """)
    fun getAllRoleIds(): List<Long>

    @Modifying
    @Query("""
        update UserRole ur
        set 
            ur.flagDefault = false,
            ur.updateUser = :updateUser,
            ur.updateDate = :updateDate
        where ur.compCd = :compCd
            and ur.flagActive is true
    """)
    fun deleteAllByCompCd(
        compCd: String,
        updateUser: String,
        updateDate: LocalDateTime ?= LocalDateTime.now()
    ): Int

    @Modifying
    @Query("""
        update UserRole ur
        set 
            ur.flagActive = false,
            ur.updateUser = :updateUser,
            ur.updateDate = :updateDate
        where ur.roleId = :roleId
            and ur.flagActive is true
    """)
    fun deleteAllByRoleId(
        roleId: Long,
        updateUser: String,
        updateDate: LocalDateTime ?= LocalDateTime.now()
    ): Int
}