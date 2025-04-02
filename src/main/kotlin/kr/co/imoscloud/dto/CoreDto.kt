package kr.co.imoscloud.dto

import kr.co.imoscloud.entity.user.User

data class SummaryMaps(
    val userMap: Map<String, User?>?,
    val roleMap: Map<Long, RoleSummery?>?,
    val companyMap: Map<String, CompanySummery?>?
)