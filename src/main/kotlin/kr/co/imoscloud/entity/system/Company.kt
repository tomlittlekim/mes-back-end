package kr.co.imoscloud.entity.system

import jakarta.persistence.*
import kr.co.imoscloud.dto.CompanyDto
import kr.co.imoscloud.dto.CompanySummery
import kr.co.imoscloud.entity.CommonCol
import kr.co.imoscloud.iface.DtoCompCdBase
import kr.co.imoscloud.iface.DtoLoginIdBase
import kr.co.imoscloud.security.UserPrincipal
import kr.co.imoscloud.util.DateUtils
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
    override var loginId: String,

    @Column(name = "PHONE_NUMBER", length = 11)
    var phoneNumber: String? = null,

    @Column(name = "DEFAULT_USER_PWD", length = 100)
    var defaultUserPwd: String? = null,

    @Column(name = "WORK_START_TIME")
    var workStartTime: String? = null,

    @Column(name = "WORK_END_TIME")
    var workEndTime: String? = null,

    ) : CommonCol(), DtoLoginIdBase, DtoCompCdBase {

        companion object {
            fun create(req: CompanyDto, randomPwd: String): Company = Company(
                site = req.site!!,
                compCd = req.compCd!!,
                businessRegistrationNumber = req.businessRegistrationNumber!!,
                corporateRegistrationNumber = req.corporateRegistrationNumber!!,
                companyName = req.companyName!!,
                imagePath = req.imagePath,
                businessAddress = req.businessAddress,
                businessType = req.businessType,
                businessItem = req.businessItem,
                flagSubscription = req.flagSubscription,
                phoneNumber = req.phoneNumber,
                loginId = "temp",
                defaultUserPwd = req.defaultUserPwd ?: randomPwd
            )

            fun toSummery(company: Company): CompanySummery = CompanySummery(
                company.id,
                company.compCd,
                company.companyName,
                company.defaultUserPwd ?: "1234"
            )
        }

        fun modify(req: CompanyDto, loginUser: UserPrincipal): Company = this.apply {
            site = req.site ?: this.site
            imagePath = req.imagePath ?: this.imagePath
            businessAddress = req.businessAddress ?: this.businessAddress
            businessType = req.businessType ?: this.businessType
            businessItem = req.businessItem ?: this.businessItem
            paymentDate = DateUtils.parseDateTime(req.paymentDate) ?: this.paymentDate
            expiredDate = DateUtils.parseDateTime(req.expiredDate) ?: this.expiredDate
            flagSubscription = req.flagSubscription
            phoneNumber = req.phoneNumber ?: this.phoneNumber
            defaultUserPwd = req.defaultUserPwd ?: this.defaultUserPwd
            updateCommonCol(loginUser)
        }
    }