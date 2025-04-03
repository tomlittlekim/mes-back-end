package kr.co.imoscloud.entity.standardInfo

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
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
    var telNo: String? = null
):CommonCol()