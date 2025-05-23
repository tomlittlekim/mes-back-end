package kr.co.imoscloud.repository.user

import kr.co.imoscloud.entity.user.MenuRole
import org.springframework.data.jpa.repository.JpaRepository

interface MenuRoleRepository: JpaRepository<MenuRole, Long> {
    fun findByRoleIdAndMenuId(roleId: Long, menuId: String): MenuRole?
}