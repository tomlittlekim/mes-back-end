package kr.co.imoscloud.entity.standardInfo

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import java.time.LocalDate

@Entity
@Table(name = "FACTORY")
class Factory (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var id: Int? = null,

    @Column(name = "SITE", nullable = false, length = 20)
    var site: String? = null,

    @Column(name = "COMP_CD", nullable = false, length = 20)
    var compCd: String? = null,

    @Column(name = "FACTORY_ID", nullable = false, length = 100)
    var factoryId: String? = null,

    @Column(name = "FACTORY_NAME", length = 100)
    var factoryName: String? = null,

    @Column(name = "FACTORY_CODE", length = 20)
    var factoryCode: String? = null,

    @Column(name = "ADDRESS", length = 200)
    var address: String? = null,

    @Column(name = "TEL_NO", length = 100)
    var telNo: String? = null,

    @Column(name = "OFFICER_NAME", length = 100)
    var officerName: String? = null,
):CommonCol()