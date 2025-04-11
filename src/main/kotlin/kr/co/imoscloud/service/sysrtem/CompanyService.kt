package kr.co.imoscloud.service.sysrtem

import jakarta.transaction.Transactional
import kr.co.imoscloud.core.Core
import kr.co.imoscloud.dto.CompanyDto
import kr.co.imoscloud.dto.CompanySummery
import kr.co.imoscloud.entity.system.Company
import kr.co.imoscloud.util.AuthLevel
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import java.util.*

@Service
class CompanyService(
    private val core: Core
) {

    fun getCompaniesForSelect(): List<CompanySummery?> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return if (core.isDeveloper(loginUser)) {
            val companyMap: MutableMap<String, CompanySummery?> = core.getAllCompanyMap(loginUser)
            if (companyMap.size == 1) core.companyRepo.findAll().map { core.companyToSummery(it) }
            else companyMap.values.toList()
        } else {
            core.getAllCompanyMap(loginUser)
                .filterValues { listOf(loginUser.compCd, "default").contains(it?.compCd) }
                .values.toList()
        }
    }

    fun getCompanies(): List<Company> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return if (core.isDeveloper(loginUser)) {
            core.companyRepo.findAllByFlagActiveIsTrue()
        } else {
            core.companyRepo.findAllByCompCdAndFlagActiveIsTrue(loginUser.compCd)
        }
    }

    fun getCompanyDetails(): Company {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        if (core.isDeveloper(loginUser)) throw IllegalArgumentException("개발자는 이용할 수 없는 서비스")

        return core.companyRepo.findByCompCdAndFlagActiveIsTrue(loginUser.compCd)
            ?: throw IllegalArgumentException("회사의 정보를 찾을 수 없습니다. ")
    }

    @AuthLevel(minLevel = 3)
    fun upsertCompany(req: CompanyDto): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        var upsertStr: String? = null
        val company: Company = try {
            req.id
                ?.let { id ->
                    core.companyRepo.findByIdAndFlagActiveIsTrue(id)
                        ?.apply {
                            upsertStr = "생성"
                            if (compCd != loginUser.compCd && !core.isDeveloper(loginUser)) {
                                throw IllegalArgumentException("회사정보를 수정할 수 있는 사용자가 아닙니다.")
                            }

                            site = req.site ?: site
                            imagePath = req.imagePath ?: imagePath
                            businessAddress = req.businessAddress ?: businessAddress
                            businessType = req.businessType ?: businessType
                            businessItem = req.businessItem ?: businessItem
                            paymentDate = req.paymentDate ?: paymentDate
                            expiredDate = req.expiredDate ?: expiredDate
                            flagSubscription = req.flagSubscription
                            phoneNumber = req.phoneNumber ?: phoneNumber
                            defaultUserPwd = req.defaultUserPwd ?: defaultUserPwd
                            updateCommonCol(loginUser)
                        }
                        ?: throw IllegalArgumentException("변경할 회사의 정보를 찾을 수 없습니다. ")
                }
                ?: run {
                    val randomPwd = UUID.randomUUID().toString().replace("-", "").substring(0, 16)
                    upsertStr = "수정"
                    Company(
                        site = req.site!!,
                        compCd = req.compCd!!,
                        businessRegistrationNumber = req.businessRegistrationNumber!!,
                        corporateRegistrationNumber = req.corporateRegistrationNumber!!,
                        companyName = req.compName!!,
                        imagePath = req.imagePath,
                        businessAddress = req.businessAddress,
                        businessType = req.businessType,
                        businessItem = req.businessItem,
                        paymentDate = req.paymentDate,
                        expiredDate = req.expiredDate,
                        flagSubscription = req.flagSubscription,
                        loginId = req.loginId!!,
                        phoneNumber = req.phoneNumber,
                        defaultUserPwd = req.defaultUserPwd ?: randomPwd
                    ).apply { createCommonCol(loginUser) }
                }
        } catch (e: NullPointerException) {
            throw IllegalArgumentException("회사를 생성하는데 필요한 정보 누락이 존재합니다. ")
        }

        core.companyRepo.save(company)
        core.upsertFromInMemory(company)
        return "${company.companyName} 회사 $upsertStr 성공"
    }

    @AuthLevel(minLevel = 5)
    @Transactional
    fun deleteCompany(id: Long): Unit {
        val target = core.companyRepo.findByIdAndFlagActiveIsTrue(id)
            ?.apply {
                flagActive = false
                updateCommonCol(SecurityUtils.getCurrentUserPrincipal())
            }
            ?: throw IllegalArgumentException("삭제할 객체가 존재하지 않습니다. ")

        core.companyRepo.save(target)
        core.userRepo.deleteAllbyCompCd(target.compCd)
        core.roleRepo.deleteAllByCompCd(target.compCd)
    }
}