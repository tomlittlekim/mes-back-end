package kr.co.imoscloud.repository.system

import kr.co.imoscloud.entity.system.MenuRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface MenuRoleRepository: JpaRepository<MenuRole, Long> {
    fun findByRoleIdAndMenuId(roleId: Long, menuId: String): MenuRole?
    fun findAllByRoleId(roleId: Long): List<MenuRole>
    fun findByIdIn(ids: List<Long>): List<MenuRole>

    @Modifying
    @Query("""
        delete from MenuRole mr
        where mr.menuId= :menuId
    """)
    fun deleteAllByMenuId(menuId: String)
}