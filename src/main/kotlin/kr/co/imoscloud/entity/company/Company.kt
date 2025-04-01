package kr.co.imoscloud.entity.company

import jakarta.persistence.*
import kr.co.imoscloud.entity.CommonCol
import java.time.LocalDateTime

@Entity
@Table(name = "COMPANY")
class Company(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    val id: Long = 0L,

    @Column(name = "SITE", length = 20, nullable = false)
    val site: String,

    @Column(name = "COMP_CD", length = 20, nullable = false)
    val compCd: String,

    @Column(name = "BUSINESS_REGISTRATION_NUMBER", length = 40, unique = true)
    val businessRegistrationNumber: String,

    @Column(name = "CORPORATE_REGISTRATION_NUMBER", length = 40, unique = true)
    val corporateRegistrationNumber: String,

    @Column(name = "COMPANY_NAME", length = 100)
    val companyName: String? = null,

    @Column(name = "IMAGE_PATH", length = 50)
    val imagePath: String? = null,

    @Column(name = "BUSINESS_ADDRESS", length = 200)
    val businessAddress: String? = null,

    @Column(name = "BUSINESS_TYPE", length = 40)
    val businessType: String? = null,

    @Column(name = "BUSINESS_ITEM", length = 40)
    val businessItem: String? = null,

    @Column(name = "PAYMENT_DATE")
    val paymentDate: LocalDateTime? = null,

    @Column(name = "EXPIRED_DATE")
    val expiredDate: LocalDateTime? = null,

    @Column(name = "FLAG_SUBSCRIPTION")
    val flagSubscription: Boolean = false,

    @Column(name = "ONER_ID", length = 40)
    val onerId: String,

    @Column(name = "PHONE_NUMBER", length = 11)
    val phoneNumber: String? = null

) : CommonCol()