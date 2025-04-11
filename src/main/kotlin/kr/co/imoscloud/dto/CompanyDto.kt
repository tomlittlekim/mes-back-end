package kr.co.imoscloud.dto

data class CompanySearchCondition(
    val site: String?=null,
    val companyName: String?=null,
)

data class CompanySummery(
    val id: Long,
    val compCd: String,
    val companyName: String?,
    val defaultUserPwd: String
)

data class CompanyDto(
    val id: Long? = null,
    val site: String? = null,
    val compCd: String? = null,
    val companyName: String? = null,
    val businessRegistrationNumber: String? = null,
    val corporateRegistrationNumber: String? = null,
    val companyStatus: String? = null,
    val imagePath: String? = null,
    val businessAddress: String? = null,
    val businessType: String? = null,
    val businessItem: String? = null,
    val paymentDate: String? = null,
    val expiredDate: String? = null,
    val flagSubscription: Boolean = false,
    val loginId: String? = null,
    val phoneNumber: String? = null,
    val defaultUserPwd: String? = null
)