package kr.co.imoscloud.repository.company

import kr.co.imoscloud.entity.company.Company
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CompanyRepository: JpaRepository<Company, Long> {
    fun findByCompCd(compCd: String): Optional<Company>
    fun findAllByCompCdIn(compCdList: List<String>): List<Company>
}