package kr.co.imoscloud.entity.system

import jakarta.persistence.*

@Entity
@Table(name = "MENU")
class Menu(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    val id: Long = 0,

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
)