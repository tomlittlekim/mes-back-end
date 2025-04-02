package kr.co.imoscloud.repository.user

import kr.co.imoscloud.entity.user.UserRole
import org.springframework.data.jpa.repository.JpaRepository

interface UserRoleRepository: JpaRepository<UserRole, Long> {
    fun findAllByRoleIdIn(idList: List<Long>): List<UserRole>
}