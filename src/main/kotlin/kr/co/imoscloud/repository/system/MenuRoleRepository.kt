package kr.co.imoscloud.repository.system

import kr.co.imoscloud.entity.system.MenuRole
import org.springframework.data.jpa.repository.JpaRepository

interface MenuRoleRepository: JpaRepository<MenuRole, Long> {
    fun findByRoleIdAndMenuId(roleId: Long, menuId: String): MenuRole?
    fun findAllByRoleId(roleId: Long): List<MenuRole>
}