package kr.co.imoscloud.dto

data class SummaryMaps(
    val userMap: Map<String, UserSummery?>?,
    val roleMap: Map<Long, RoleSummery?>?,
    val companyMap: Map<String, CompanySummery?>?
)