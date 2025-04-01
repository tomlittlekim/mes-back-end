package kr.co.imoscloud.entity.user

import jakarta.persistence.*

@Entity
@Table(name = "MENU_ROLE")
class MenuRole(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

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
    var isUpdate: Boolean = false
)