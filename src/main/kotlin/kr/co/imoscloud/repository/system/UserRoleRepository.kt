package kr.co.imoscloud.repository.system

import kr.co.imoscloud.entity.system.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface UserRoleRepository: JpaRepository<UserRole, Long> {
    fun findAllByRoleIdIn(idList: List<Long?>): List<UserRole>

    @Query("""
        select ur
        from UserRole ur
        where (ur.compCd = 'default' or ur.compCd = :compCd)
            and ur.flagActive is true 
        order by ur.sequence desc
    """)
    fun getRolesByCompany(compCd: String): List<UserRole>

    fun findAllByFlagActiveIsTrue(): List<UserRole>
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
        set ur.flagDefault = false
        where ur.compCd = :compCd
    """)
    fun deleteAllByCompCd(compCd: String)
}