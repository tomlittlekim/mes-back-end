package kr.co.imoscloud.service.sysrtem

import jakarta.transaction.Transactional
import kr.co.imoscloud.core.CompanyCacheManager
import kr.co.imoscloud.core.UserCacheManager
import kr.co.imoscloud.core.UserRoleCacheManager
import kr.co.imoscloud.dto.CompanyDto
import kr.co.imoscloud.dto.CompanySearchCondition
import kr.co.imoscloud.dto.CompanySummery
import kr.co.imoscloud.entity.system.Company
import kr.co.imoscloud.security.UserPrincipal
import kr.co.imoscloud.util.AuthLevel
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import java.util.*

@Service
class CompanyService(
    private val ccm: CompanyCacheManager,
    private val ucm: UserCacheManager,
    private val rcm: UserRoleCacheManager,
    private val userService: UserService,
) {

    fun getCompaniesForSelect(): List<CompanySummery?> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return if (rcm.isDeveloper(loginUser)) {
            val companyMap: Map<String, CompanySummery?> = ccm.getCompanies(listOf(loginUser.compCd))
            if (companyMap.size == 1) ccm.companyRepo.findAll().map { it.toSummery() }
            else companyMap.values.toList()
        } else {
            ccm.getCompanies(listOf(loginUser.compCd))
                .filterValues { listOf(loginUser.compCd, "default").contains(it?.compCd) }
                .values.toList()
        }
    }

    fun getCompanies(req: CompanySearchCondition): List<Company> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return if (rcm.isDeveloper(loginUser)) {
            ccm.companyRepo.findAllBySearchConditionForDev(req.site, "%${req.companyName?:""}%")
        } else {
            ccm.companyRepo.findAllBySearchConditionForExceptDev(loginUser.compCd)
        }
    }

    fun getCompanyDetails(): Company {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        if (rcm.isDeveloper(loginUser)) throw IllegalArgumentException("개발자는 이용할 수 없는 서비스")

        return ccm.companyRepo.findByCompCdAndFlagActiveIsTrue(loginUser.compCd)
            ?: throw IllegalArgumentException("회사의 정보를 찾을 수 없습니다. ")
    }

    @AuthLevel(minLevel = 3)
    @Transactional
    fun upsertCompany(req: CompanyDto): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        var upsertStr: String? = null
        val company: Company = try {
            req.id
                ?.let { id ->
                    ccm.companyRepo.findByIdAndFlagActiveIsTrue(id)
                        ?.let { company ->
                            upsertStr = "생성"
                            if (req.compCd != loginUser.compCd && !rcm.isDeveloper(loginUser))
                                throw IllegalArgumentException("회사정보를 수정할 수 있는 사용자가 아닙니다.")

                            company.modify(req, loginUser)
                        }
                        ?: throw IllegalArgumentException("변경할 회사의 정보를 찾을 수 없습니다. ")
                }
                ?: run {
                    upsertStr = "수정"
                    val randomPwd = UUID.randomUUID().toString().replace("-", "").substring(0, 16)
                    val company = Company.create(req, randomPwd).apply { createCommonCol(loginUser) }

                    val owner = userService.generateOwner(company)
                    company.apply { loginId = owner.loginId }
                }
        } catch (e: NullPointerException) {
            throw IllegalArgumentException("회사를 생성하는데 필요한 정보 누락이 존재합니다. ")
        }

        ccm.saveAllAndSyncCache(listOf(company))
        return "${company.companyName} 회사 $upsertStr 성공"
    }

    @AuthLevel(minLevel = 5)
    @Transactional
    fun deleteCompany(compCd: String): Boolean {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        ccm.getCompany(compCd)
            ?.let { cs: CompanySummery ->
                if (cs.compCd != loginUser.compCd && !rcm.isDeveloper(loginUser))
                    throw IllegalArgumentException("회사정보를 수정할 수 있는 사용자가 아닙니다.")

                ccm.softDeleteAndSyncCache(cs, loginUser.loginId)
                ucm.softDeleteAllByCompCdAndSyncCache(cs.compCd, loginUser.loginId)
                rcm.softDeleteAllByCompCdAndSyncCache(cs.compCd, loginUser.loginId)
            }
            ?: throw IllegalArgumentException("회사 정보가 존재하지 않습니다. ")

        return true
    }


    //TODO: site 변경
    fun getWorkTime(userPrincipal: UserPrincipal): Pair<String,String> {
        val company = ccm.companyRepo.findBySiteAndCompCdAndFlagActiveIsTrue(
            site = "gyeonggi",
            compCd = userPrincipal.compCd
        )

        return Pair(
            company?.workStartTime?.substring(0,2) ?:throw Exception("해당 기업의 업무 시작 시간이 존재하지 않습니다."),
            company.workEndTime?.substring(0,2) ?:throw Exception("해당 기업의 업무 종료 시간이 존재하지 않습니다. "),
        )
    }
}