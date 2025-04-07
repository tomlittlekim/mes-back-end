package kr.co.imoscloud.fetcher.company

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import kr.co.imoscloud.entity.company.Company
import kr.co.imoscloud.service.company.companyService

@DgsComponent
class companyFetcher(
    private val companyService: companyService
) {

    @DgsQuery
    fun getCompanySelect(): List<Company> = companyService.getCompanySelect()
}