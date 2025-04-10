package kr.co.imoscloud.service.sysrtem

import kr.co.imoscloud.core.Core
import kr.co.imoscloud.dto.CompanyDto
import kr.co.imoscloud.entity.system.Company
import kr.co.imoscloud.util.AuthLevel
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service

@Service
class CompanyService(
    private val core: Core
) {

    fun getCompanies(): List<Company>  = core.companyRepo.findAllByFlagActiveIsTrue()

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
                            updateCommonCol(loginUser)
                        }
                        ?: throw IllegalArgumentException("변경할 회사의 정보를 찾을 수 없습니다. ")
                }
                ?: run {
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
                        phoneNumber = req.phoneNumber
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
    fun deleteCompany(id: Long): Unit {
        val target = core.companyRepo.findByIdAndFlagActiveIsTrue(id)
            ?.apply {
                flagActive = false
                updateCommonCol(SecurityUtils.getCurrentUserPrincipal())
            }
            ?: throw IllegalArgumentException("삭제할 객체가 존재하지 않습니다. ")

        core.companyRepo.save(target)
    }
}