package kr.co.imoscloud.entity.standardInfo

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "VENDOR")
class Vendor (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var id: Int? = null,

    @Column(name = "SITE", nullable = false, length = 20)
    var site: String? = null,

    @Column(name = "COMP_CD", nullable = false, length = 20)
    var compCd: String? = null,

    @Column(name = "VENDOR_ID", nullable = false, length = 100)
    var vendorId: String? = null,

    @Column(name = "VENDOR_NAME", length = 100)
    var vendorName: String? = null,

    @Column(name = "VENDOR_TYPE", length = 100)
    var vendorType: String? = null,

    @Column(name = "BUSINESS_REG_NO", length = 100)
    var businessRegNo: String? = null,

    @Column(name = "CEO_NAME", length = 50)
    var ceoName: String? = null,

    @Column(name = "BUSINESS_TYPE", length = 50)
    var businessType: String? = null,

    @Column(name = "ADDRESS", length = 200)
    var address: String? = null,

    @Column(name = "TEL", length = 20)
    var telNo: String? = null,

    @Column(name = "FLAG_ACTIVE")
    var flagActive: Boolean? = null,

    @Column(name = "CREATE_USER", length = 100)
    var createUser: String? = null,

    @Column(name = "CREATE_DATE")
    var createDate: LocalDate? = null,

    @Column(name = "UPDATE_USER", length = 100)
    var updateUser: String? = null,

    @Column(name = "UPDATE_DATE")
    var updateDate: LocalDate? = null
)