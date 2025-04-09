package kr.co.imoscloud.repository.system

import kr.co.imoscloud.entity.system.Menu
import org.springframework.data.jpa.repository.JpaRepository

interface MenuRepository : JpaRepository<Menu, Long> {
    fun findByIdAndFlagActiveIsTrue(id: Long): Menu?
}