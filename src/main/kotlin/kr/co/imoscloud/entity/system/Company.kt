package kr.co.imoscloud.entity.system

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import kr.co.imoscloud.iface.DtoCompCdBase
import kr.co.imoscloud.iface.DtoLoginIdBase
import java.time.LocalDateTime

@Entity
@Table(name = "COMPANY")
class Company(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    val id: Long = 0L,

    @Column(name = "SITE", length = 20, nullable = false)
    var site: String,

    @Column(name = "COMP_CD", length = 20, nullable = false, unique = true)
    override val compCd: String,

    @Column(name = "BUSINESS_REGISTRATION_NUMBER", length = 40, unique = true)
    val businessRegistrationNumber: String,

    @Column(name = "CORPORATE_REGISTRATION_NUMBER", length = 40, unique = true)
    val corporateRegistrationNumber: String,

    @Column(name = "COMPANY_NAME", length = 100)
    var companyName: String? = null,

    @Column(name = "IMAGE_PATH", length = 50)
    var imagePath: String? = null,

    @Column(name = "BUSINESS_ADDRESS", length = 200)
    var businessAddress: String? = null,

    @Column(name = "BUSINESS_TYPE", length = 40)
    var businessType: String? = null,

    @Column(name = "BUSINESS_ITEM", length = 40)
    var businessItem: String? = null,

    @Column(name = "PAYMENT_DATE")
    var paymentDate: LocalDateTime? = null,

    @Column(name = "EXPIRED_DATE")
    var expiredDate: LocalDateTime? = null,

    @Column(name = "FLAG_SUBSCRIPTION")
    var flagSubscription: Boolean = false,

    @Column(name = "ONER_ID", length = 40)
    override val loginId: String,

    @Column(name = "PHONE_NUMBER", length = 11)
    var phoneNumber: String? = null,

    @Column(name = "DEFAULT_USER_PWD", length = 100)
    var defaultUserPwd: String? = null

    ) : CommonCol(), DtoLoginIdBase, DtoCompCdBase