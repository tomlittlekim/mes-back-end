package kr.co.imoscloud.repository.system

import kr.co.imoscloud.entity.system.KpiCategoryMaster
import kr.co.imoscloud.entity.system.KpiCompanySubscription
import kr.co.imoscloud.entity.system.KpiIndicatorMaster
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface KpiCategoryRepository : JpaRepository<KpiCategoryMaster, Long> {
    fun findAllByFlagActiveTrue(): List<KpiCategoryMaster>
    
    fun findByCategoryCdAndFlagActiveTrue(categoryCd: String): KpiCategoryMaster?
    
    // 전체 카테고리 조회 (관리자용)
    @Query("SELECT k FROM KpiCategoryMaster k WHERE k.flagActive = true")
    fun findAllCategories(): List<KpiCategoryMaster>
}

@Repository
interface KpiIndicatorRepository : JpaRepository<KpiIndicatorMaster, Long> {
    fun findAllByFlagActiveTrue(): List<KpiIndicatorMaster>
    
    fun findByKpiIndicatorCdAndFlagActiveTrue(kpiIndicatorCd: String): KpiIndicatorMaster?
    
    // 모든 KPI 지표 조회 (관리자용)
    @Query("SELECT k FROM KpiIndicatorMaster k WHERE k.flagActive = true")
    fun findAllIndicators(): List<KpiIndicatorMaster>
}

@Repository
interface KpiSubscriptionRepository : JpaRepository<KpiCompanySubscription, Long> {
    fun findAllByFlagActiveTrue(): List<KpiCompanySubscription>
    
    fun findBySiteAndCompCdAndFlagActiveTrue(site: String, compCd: String): List<KpiCompanySubscription>
    
    fun findBySiteAndCompCdAndKpiIndicatorCdAndFlagActiveTrue(site: String, compCd: String, kpiIndicatorCd: String): KpiCompanySubscription?
    
    // 모든 구독 정보 조회 (관리자용)
    @Query("SELECT k FROM KpiCompanySubscription k WHERE k.flagActive = true")
    fun findAllSubscriptions(): List<KpiCompanySubscription>
    
    // 특정 회사의 모든 구독 정보 조회 (활성화 여부 상관없이)
    @Query("SELECT k FROM KpiCompanySubscription k WHERE k.site = :site AND k.compCd = :compCd")
    fun findSubscriptionsBySiteAndCompCd(@Param("site") site: String, @Param("compCd") compCd: String): List<KpiCompanySubscription>
} 