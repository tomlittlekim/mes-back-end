package kr.co.imoscloud.repository.system

import kr.co.imoscloud.entity.system.MenuRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface MenuRoleRepository: JpaRepository<MenuRole, Long> {
    fun findByRoleIdAndMenuId(roleId: Long, menuId: String): MenuRole?
    fun findAllByRoleId(roleId: Long): List<MenuRole>
    fun findByIdIn(ids: List<Long>): List<MenuRole>
    fun findAllByMenuId(menuId: String): List<MenuRole>

    @Modifying
    @Query("""
        delete from MenuRole mr
        where mr.menuId= :menuId
    """)
    fun deleteAllByMenuId(menuId: String)

    @Modifying
    @Query("""
        delete from MenuRole mr
        where mr.roleId= :roleId
    """)
    fun deleteAllByRoleId(roleId: Long)

    @Modifying
    @Query("""
        update MenuRole mr
        set mr.flagCategory = :flagCategory
        where mr.menuId = :menuId
    """)
    fun updateAllByMenuId(menuId: String, flagCategory: Boolean): Int
}