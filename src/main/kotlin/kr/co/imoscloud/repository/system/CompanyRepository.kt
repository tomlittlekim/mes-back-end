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
        where (:site is null or c.site = :site)
            and (c.compCd = 'default' or c.compCd = :compCd)
            and (:companyName is null or c.companyName like :companyName)
            and c.flagActive is true 
    """)
    fun findAllBySearchConditionForExceptDev(compCd: String, site: String?=null, companyName: String?=null): List<Company>

    @Query("""
        select c
        from Company c
        where (:site is null or c.site = :site)
            and (:companyName is null or c.companyName like :companyName)
            and c.flagActive is true
    """)
    fun findAllBySearchConditionForDev(site: String?=null, companyName: String?=null): List<Company>

    @Query("""
        select
            m.menuName as title,
            c.businessRegistrationNumber as registrationNumber,
            c.companyName as company,
            c.loginId as owner,
            c.businessAddress as address,
            c.businessType as type,
            c.businessItem as item,
            c.phoneNumber as number
        from Company c
        left join Menu m on m.menuId = :menuId
            and m.flagActive is true
        where c.compCd = :compCd
            and c.flagActive is true
    """)
    fun getInitialHeader(site: String, compCd: String, menuId: String): Map<String, String>
}