package kr.co.imoscloud.repository.system

import kr.co.imoscloud.entity.kpi.KpiCategoryMaster
import kr.co.imoscloud.entity.kpi.KpiCompanySubscription
import kr.co.imoscloud.entity.kpi.KpiIndicatorMaster
import kr.co.imoscloud.model.kpisetting.KpiIndicatorWithCategoryAndSubscriptionModel
import kr.co.imoscloud.model.kpisetting.KpiIndicatorWithCategoryModel
import kr.co.imoscloud.model.kpisetting.KpiSubscriptionModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface KpiCategoryRepository : JpaRepository<KpiCategoryMaster, Long> {
    // 전체 카테고리 조회 (관리자용)
    @Query("SELECT k FROM KpiCategoryMaster k WHERE k.flagActive = true")
    fun findAllCategories(): List<KpiCategoryMaster>
}

@Repository
interface KpiIndicatorRepository : JpaRepository<KpiIndicatorMaster, Long> {
    // 모든 KPI 지표 조회 (관리자용)
    @Query("SELECT k FROM KpiIndicatorMaster k WHERE k.flagActive = true")
    fun findAllIndicators(): List<KpiIndicatorMaster>
    
    // KPI 지표와 카테고리 정보를 함께 조회
    @Query("""
        SELECT new kr.co.imoscloud.model.kpisetting.KpiIndicatorWithCategoryModel(
            i.kpiIndicatorCd, i.kpiIndicatorNm, i.description, 
            i.categoryCd, c.categoryNm, i.unit, i.chartType
        )
        FROM KpiIndicatorMaster i
        LEFT JOIN KpiCategoryMaster c ON i.categoryCd = c.categoryCd AND i.site = c.site AND i.compCd = c.compCd
        WHERE i.flagActive = true
    """)
    fun findAllIndicatorsWithCategory(): List<KpiIndicatorWithCategoryModel>
}

@Repository
interface KpiSubscriptionRepository : JpaRepository<KpiCompanySubscription, Long> {
    // 모든 구독 정보 조회 (관리자용)
    @Query("SELECT k FROM KpiCompanySubscription k WHERE k.flagActive = true")
    fun findAllSubscriptions(): List<KpiCompanySubscription>
    
    // 특정 회사의 모든 구독 정보 조회 (활성화 여부 상관없이)
    @Query("SELECT k FROM KpiCompanySubscription k WHERE k.site = :site AND k.compCd = :compCd")
    fun findSubscriptionsBySiteAndCompCd(@Param("site") site: String, @Param("compCd") compCd: String): List<KpiCompanySubscription>
    
    // 특정 회사의 활성화된 구독 정보만 조회
    @Query("""
        SELECT new kr.co.imoscloud.model.kpisetting.KpiSubscriptionModel(
            k.site, k.compCd, k.kpiIndicatorCd, k.categoryId, k.targetValue ,k.description, k.sort, k.flagActive
        )
        FROM KpiCompanySubscription k 
        WHERE k.site = :site 
        AND k.compCd = :compCd 
        AND k.flagActive = true
    """)
    fun findActiveSubscriptionsBySiteAndCompCd(
        @Param("site") site: String, 
        @Param("compCd") compCd: String
    ): List<KpiSubscriptionModel>

    @Query("""
    SELECT new kr.co.imoscloud.model.kpisetting.KpiIndicatorWithCategoryAndSubscriptionModel(
        i.kpiIndicatorCd, i.kpiIndicatorNm, i.description, 
        i.categoryCd, c.categoryNm, i.unit, i.chartType,
        s.targetValue, s.flagActive
    )
    FROM KpiIndicatorMaster i
    LEFT JOIN KpiCategoryMaster c ON i.categoryCd = c.categoryCd AND i.site = c.site AND i.compCd = c.compCd
    LEFT JOIN KpiCompanySubscription s ON i.kpiIndicatorCd = s.kpiIndicatorCd AND s.site = :site AND s.compCd = :compCd
    WHERE i.flagActive = true
    """)
    fun findAllIndicatorsWithCategoryAndSubscription(
        @Param("site") site: String,
        @Param("compCd") compCd: String
    ): List<KpiIndicatorWithCategoryAndSubscriptionModel>
} 