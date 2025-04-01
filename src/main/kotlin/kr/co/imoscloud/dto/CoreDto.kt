package kr.co.imoscloud.dto

data class SummaryMaps(
    val userMap: Map<Long, String?>?,
    val roleMap: Map<Long, RoleSummery?>?,
    val companyMap: Map<String, CompanySummery?>?
)