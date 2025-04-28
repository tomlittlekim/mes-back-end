package kr.co.imoscloud.entity.drive

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol

@Entity
@Table(name = "FILE_MANAGEMENT")
class FileManagement(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FILE_ID")
    val id: Long = 0,

    @Column(name = "FILE_NAME", unique = true, nullable = false, length = 100)
    var name: String,

    @Column(name = "FILE_EXTENSION", unique = true, nullable = false, length = 100)
    var extension: String,

    @Column(name = "FILE_PATH", unique = true, nullable = false, length = 100)
    var path: String,

    @Column(name = "FILE_SIZE")
    var size: Int=0,

    @Column(name = "MENU_ID")
    var menuId: String? = null,

): CommonCol()