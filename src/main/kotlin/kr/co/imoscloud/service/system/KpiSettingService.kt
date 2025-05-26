package kr.co.imoscloud.service.system

import kr.co.imoscloud.dto.CompanySearchCondition
import kr.co.imoscloud.entity.system.KpiCompanySubscription
import kr.co.imoscloud.model.kpisetting.*
import kr.co.imoscloud.repository.CodeRep
import kr.co.imoscloud.repository.system.KpiCategoryRepository
import kr.co.imoscloud.repository.system.KpiIndicatorRepository
import kr.co.imoscloud.repository.system.KpiSubscriptionRepository
import kr.co.imoscloud.util.AuthLevel
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KpiSettingService(
    private val kpiCategoryRepository: KpiCategoryRepository,
    private val kpiIndicatorRepository: KpiIndicatorRepository,
    private val kpiSubscriptionRepository: KpiSubscriptionRepository,
    private val companyService: CompanyService,
    private val codeRep: CodeRep
) {
    
    /**
     * 지점 및 회사 정보 조회
     * 관리자만 접근 가능 (권한 레벨 5 이상)
     */
    @AuthLevel(minLevel = 5)
    fun getBranchCompanies(): List<BranchModel> {
        // DB에서 회사 정보 조회
        val companies = companyService.getCompanies(CompanySearchCondition(site = null, companyName = null))
        
        // 지점(site)별로 그룹화
        val companiesByBranch = companies.groupBy { it.site }
        
        // 결과 변환
        return companiesByBranch.map { (site, companyList) ->
            BranchModel(
                id = site,
                name = getBranchName(site),
                companies = companyList.map { company ->
                    KpiCompanyModel(
                        id = company.compCd,
                        name = company.companyName ?: ""
                    )
                }
            )
        }
    }
    
    /**
     * 지점 ID에 해당하는 지점명 반환
     * CHAPTER 코드 클래스에서 지점 코드 조회
     */
    private fun getBranchName(siteId: String): String {
        val chapterCodes = codeRep.getInitialCodes(codeClassId = "CHAPTER")
        val siteNameMap = chapterCodes.associate { code ->
            (code?.codeId ?: "") to (code?.codeName ?: "")
        }
        return siteNameMap[siteId] ?: siteId
    }
    
    /**
     * KPI 지표 정보 조회
     * 관리자만 접근 가능 (권한 레벨 5 이상)
     */
    @AuthLevel(minLevel = 5)
    fun getKpiIndicators(): List<KpiIndicatorModel> {
        // DB에서 KPI 지표 정보 조회
        val indicators = kpiIndicatorRepository.findAllIndicators()
        
        // 카테고리 정보 조회
        val categories = kpiCategoryRepository.findAllCategories().associateBy { it.categoryCd }
        
        // 결과 변환
        return indicators.map { indicator ->
            KpiIndicatorModel(
                kpiIndicatorCd = indicator.kpiIndicatorCd,
                kpiIndicatorNm = indicator.kpiIndicatorNm ?: "",
                description = indicator.description,
                categoryCd = indicator.categoryCd,
                categoryNm = categories[indicator.categoryCd]?.categoryNm ?: indicator.categoryCd,
                targetValue = indicator.targetValue,
                unit = indicator.unit,
                chartType = indicator.chartType
            )
        }
    }
    
    /**
     * 회사별 KPI 구독 정보 조회
     * 관리자만 접근 가능 (권한 레벨 5 이상)
     */
    @AuthLevel(minLevel = 5)
    fun getKpiSubscriptions(): List<KpiSubscriptionModel> {
        // DB에서 조회
        val subscriptions = kpiSubscriptionRepository.findAllSubscriptions()
        
        return subscriptions.map { subscription -> 
            KpiSubscriptionModel(
                site = subscription.site,
                compCd = subscription.compCd,
                kpiIndicatorCd = subscription.kpiIndicatorCd,
                categoryId = subscription.categoryId,
                description = subscription.description,
                sort = subscription.sort,
                flagActive = subscription.flagActive
            )
        }
    }
    
    /**
     * KPI 설정 저장 (프론트에서 해당 site와 compCd에 대한 전체 데이터 전송)
     * 관리자만 접근 가능 (권한 레벨 5 이상)
     */
    @Transactional(rollbackFor = [Exception::class])
    @AuthLevel(minLevel = 5)
    fun saveKpiSettings(settings: List<KpiSettingInput>): KpiSettingResult {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()
        
        try {
            val settingsByCompany = settings.groupBy { "${it.site}_${it.compCd}" }

            val toSave = mutableListOf<KpiCompanySubscription>()
            var totalUpdated = 0
            var totalCreated = 0
            var totalUnchanged = 0
            
            settingsByCompany.forEach { (key, companySettings) ->
                val (site, compCd) = key.split("_")

                val existingSubscriptions = kpiSubscriptionRepository.findSubscriptionsBySiteAndCompCd(site, compCd)
                val existingSubscriptionsMap = existingSubscriptions.associateBy { it.kpiIndicatorCd }
                
                // 변경된 항목만 처리
                companySettings.forEach { setting ->
                    val existingSubscription = existingSubscriptionsMap[setting.kpiIndicatorCd]
                    
                    if (existingSubscription != null) {
                        // 실제 변경이 있는지 체크
                        val isChanged = existingSubscription.categoryId != setting.categoryId ||
                                       existingSubscription.description != setting.description ||
                                       existingSubscription.sort != setting.sort ||
                                       existingSubscription.flagActive != (setting.flagActive ?: false)
                        
                        if (isChanged) {
                            // 변경된 필드만 업데이트
                            existingSubscription.categoryId = setting.categoryId
                            existingSubscription.description = setting.description
                            existingSubscription.sort = setting.sort
                            existingSubscription.flagActive = setting.flagActive ?: false
                            existingSubscription.updateCommonCol(userPrincipal)
                            
                            toSave.add(existingSubscription)
                            totalUpdated++
                        } else {
                            // 변경 없음
                            totalUnchanged++
                        }
                    } else {
                        // 새로운 구독 정보 생성
                        val newSubscription = KpiCompanySubscription(
                            site = setting.site,
                            compCd = setting.compCd,
                            kpiIndicatorCd = setting.kpiIndicatorCd,
                            categoryId = setting.categoryId,
                            description = setting.description,
                            sort = setting.sort
                        )
                        
                        newSubscription.createCommonCol(userPrincipal)
                        newSubscription.flagActive = setting.flagActive ?: false
                        
                        toSave.add(newSubscription)
                        totalCreated++
                    }
                }
            }

            // 변경된 항목이 있는 경우에만 저장
            if (toSave.isNotEmpty()) {
                kpiSubscriptionRepository.saveAll(toSave)
            }
            
            return KpiSettingResult(success = true, message = "KPI 설정이 저장되었습니다. (변경: ${toSave.size}개)")
        } catch (e: Exception) {
            throw Exception("KPI 설정에 실패했습니다: ${e.message}")
        }
    }
} 