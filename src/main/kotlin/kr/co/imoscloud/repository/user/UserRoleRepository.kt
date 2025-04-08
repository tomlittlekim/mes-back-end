package kr.co.imoscloud.repository.user

import kr.co.imoscloud.entity.user.UserRole
import org.springframework.data.jpa.repository.JpaRepository
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
}