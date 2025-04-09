package kr.co.imoscloud.dto

import java.time.LocalDateTime

data class CompanySummery(
    val id: Long,
    val companyName: String?,
)

data class CompanyDto(
    val id: Long? = null,
    val site: String? = null,
    val compCd: String? = null,
    val compName: String? = null,
    val businessRegistrationNumber: String? = null,
    val corporateRegistrationNumber: String? = null,
    val companyStatus: String? = null,
    val imagePath: String? = null,
    val businessAddress: String? = null,
    val businessType: String? = null,
    val businessItem: String? = null,
    val paymentDate: LocalDateTime? = null,
    val expiredDate: LocalDateTime? = null,
    val flagSubscription: Boolean = false,
    val loginId: String? = null,
    val phoneNumber: String? = null
)