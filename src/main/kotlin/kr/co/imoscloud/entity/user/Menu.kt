package kr.co.imoscloud.entity.user

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol

@Entity
@Table(name = "MENU")
class Menu(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "MENU_ID", length = 100, nullable = false)
    val menuId: String,

    @Column(name = "MENU_NAME", length = 200, nullable = false)
    val menuName: String,

    @Column(name = "ROLE_ID", nullable = false)
    val roleId: Long,

    @Column(name = "FLAG_SUBSCRIBE")
    var flagSubscribe: Boolean = false,

    @Column(name = "FLAG_VISIBLE")
    var flagVisible: Boolean = true,

    @Column(name = "SEQUENCE")
    var sequence: Int = 0
) : CommonCol()