package kr.co.imoscloud.repository.user

import kr.co.imoscloud.entity.user.Menu
import org.springframework.data.jpa.repository.JpaRepository

interface MenuRepository : JpaRepository<Menu, Long> {
    fun findByIdAndFlagActiveIsTrue(id: Long): Menu?
}