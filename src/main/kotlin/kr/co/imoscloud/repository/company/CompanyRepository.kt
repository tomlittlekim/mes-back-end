package kr.co.imoscloud.repository.company

import kr.co.imoscloud.entity.company.Company
import org.springframework.data.jpa.repository.JpaRepository

interface CompanyRepository: JpaRepository<Long, Company> {
}