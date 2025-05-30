package kr.co.imoscloud.entity.system

import jakarta.persistence.*
import kr.co.imoscloud.dto.MenuRequest
import kr.co.imoscloud.dto.MenuRoleDto

@Entity
@Table(name = "MENU")
class Menu(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    val id: Long? = null,

    @Column(name = "MENU_ID", length = 100, nullable = false)
    var menuId: String,

    @Column(name = "UP_MENU_ID", length = 100)
    var upMenuId: String?=null,

    @Column(name = "MENU_NAME", length = 200, nullable = false)
    var menuName: String,

    @Column(name = "FLAG_SUBSCRIBE")
    var flagSubscribe: Boolean = false,

    @Column(name = "SEQUENCE")
    var sequence: Int = 0,

    @Column(name = "FLAG_ACTIVE")
    var flagActive: Boolean = true
) {
    companion object {
        fun create(req: MenuRequest): Menu = Menu(
            menuId = req.menuId!!,
            upMenuId = req.upMenuId!!,
            menuName = req.menuName!!,
            flagSubscribe = req.flagSubscribe!!,
            flagActive = req.flagActive,
            sequence = req.sequence!!
        )
    }

    fun copy(): Menu = Menu(
        id = this.id,
        menuId = menuId,
        upMenuId = this.upMenuId,
        menuName = this.menuName,
        flagSubscribe = this.flagSubscribe,
        sequence = this.sequence,
        flagActive = this.flagActive
    )

    fun toDto(roleId: Long, upMenuId: String?): MenuRoleDto = MenuRoleDto(
        roleId = roleId,
        menuId = this.menuId,
        upMenuId = upMenuId,
        flagCategory = this.upMenuId == null
    )

    fun modify(req: MenuRequest): Menu = this.apply {
        menuId = req.menuId ?: this.menuId
        upMenuId = req.upMenuId ?: this.upMenuId
        menuName = req.menuName ?: this.menuName
        flagSubscribe = req.flagSubscribe ?: this.flagSubscribe
        sequence = req.sequence ?: this.sequence
        flagActive = req.flagActive
    }
}