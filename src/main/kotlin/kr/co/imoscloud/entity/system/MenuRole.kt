package kr.co.imoscloud.entity.system

import jakarta.persistence.*
import kr.co.imoscloud.dto.MenuRoleDto

@Entity
@Table(name = "MENU_ROLE")
class MenuRole(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    val id: Long? = null,

    @Column(name = "ROLE_ID", length = 40, nullable = false)
    var roleId: Long,

    @Column(name = "MENU_ID", length = 40, nullable = false)
    var menuId: String,

    @Column(name = "IS_OPEN")
    var isOpen: Boolean = false,

    @Column(name = "IS_DELETE")
    var isDelete: Boolean = false,

    @Column(name = "IS_INSERT")
    var isInsert: Boolean = false,

    @Column(name = "IS_ADD")
    var isAdd: Boolean = false,

    @Column(name = "IS_POPUP")
    var isPopup: Boolean = false,

    @Column(name = "IS_PRINT")
    var isPrint: Boolean = false,

    @Column(name = "IS_SELECT")
    var isSelect: Boolean = false,

    @Column(name = "IS_UPDATE")
    var isUpdate: Boolean = false,

    @Column(name = "FLAG_CATEGORY")
    var flagCategory: Boolean = false
) {

    companion object {
        fun create(req: MenuRoleDto): MenuRole = MenuRole(
            roleId = req.roleId!!,
            menuId = req.menuId,
            isOpen = req.isOpen  ?: false,
            isDelete = req.isDelete ?: false,
            isInsert = req.isInsert ?: false,
            isAdd = req.isAdd ?: false,
            isPopup = req.isPopup ?: false,
            isPrint = req.isPrint ?: false,
            isSelect = req.isSelect ?: false,
            isUpdate = req.isUpdate ?: false,
        )
    }

    fun toBooleanList(): List<Boolean> = listOf(
        this.isOpen,
        this.isDelete,
        this.isInsert,
        this.isAdd,
        this.isPopup,
        this.isPrint,
        this.isSelect,
        this.isUpdate,
        this.flagCategory
    )

    fun toDto(roleId: Long, upMenuId: String?): MenuRoleDto = MenuRoleDto(
        roleId = roleId,
        menuId = this.menuId,
        upMenuId = upMenuId,
        isOpen = this.isOpen,
        isDelete = this.isDelete,
        isInsert = this.isInsert,
        isAdd = this.isAdd,
        isPopup = this.isPopup,
        isPrint = this.isPrint,
        isSelect = this.isSelect,
        isUpdate = this.isUpdate,
        flagCategory = this.flagCategory
    )

    fun modify(req: MenuRoleDto): MenuRole = this.apply {
        roleId = req.roleId ?: this.roleId
        menuId = req.menuId
        isOpen = req.isOpen ?: this.isOpen
        isDelete = req.isDelete ?: this.isDelete
        isInsert = req.isInsert ?: this.isInsert
        isAdd = req.isAdd ?: this.isAdd
        isPopup = req.isPopup ?: this.isPopup
        isPrint = req.isPrint ?: this.isPrint
        isSelect = req.isSelect ?: this.isSelect
        isUpdate = req.isUpdate ?: this.isUpdate
    }
}