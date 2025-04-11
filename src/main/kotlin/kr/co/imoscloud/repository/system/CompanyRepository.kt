package kr.co.imoscloud.repository.system

import kr.co.imoscloud.entity.system.Company
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface CompanyRepository: JpaRepository<Company, Long> {
    fun findByCompCd(compCd: String?): Optional<Company>
    fun findAllByCompCdIn(compCdList: List<String?>): List<Company>
    fun findAllByFlagActiveIsTrue(): List<Company>
    fun findByIdAndFlagActiveIsTrue(id: Long): Company?
    fun findByCompCdAndFlagActiveIsTrue(compCd: String): Company?

    @Query("""
        select c
        from Company c
        where (c.compCd = 'default' or c.compCd = :compCd)
            and c.flagActive is true 
    """)
    fun findAllByCompCdAndFlagActiveIsTrue(compCd: String): List<Company>

    @Query("""
        select c
        from Company c
        where (:site is null or c.site = :site)
            and (:companyName is null or c.companyName like :companyName)
            and c.flagActive is true
    """)
    fun findAllBySearchCondition(site: String?=null, companyName: String?=null): List<Company>
}