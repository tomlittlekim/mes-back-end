package kr.co.imoscloud.repository.system

import kr.co.imoscloud.entity.system.Company
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CompanyRepository: JpaRepository<Company, Long> {
    fun findByCompCd(compCd: String?): Optional<Company>
    fun findAllByCompCdIn(compCdList: List<String?>): List<Company>
    fun findAllByFlagActiveIsTrue(): List<Company>
    fun findByIdAndFlagActiveIsTrue(id: Long): Company?
}