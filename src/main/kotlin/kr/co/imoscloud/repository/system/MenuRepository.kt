package kr.co.imoscloud.repository.system

import kr.co.imoscloud.entity.system.Menu
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MenuRepository : JpaRepository<Menu, Long> {
    fun findByIdAndFlagActiveIsTrue(id: Long): Menu?

    @Query("""
        SELECT m FROM Menu m
        WHERE (:menuId IS NULL OR m.menuId = :menuId)
          AND (:menuName IS NULL OR m.menuName LIKE %:menuName%)
          AND m.flagActive = true
    """)
    fun findAllByParms(menuId: String?, menuName: String?): List<Menu>
}