package kr.co.imoscloud.service.company

import kr.co.imoscloud.entity.company.Company
import kr.co.imoscloud.repository.system.CompanyRepository
import kr.co.imoscloud.util.AuthLevel
import org.springframework.stereotype.Service

@Service
class companyService(
    private val companyRepo: CompanyRepository
) {

    @AuthLevel(minLevel = 5)
    fun getCompanySelect(): List<Company>  = companyRepo.findAll()
}