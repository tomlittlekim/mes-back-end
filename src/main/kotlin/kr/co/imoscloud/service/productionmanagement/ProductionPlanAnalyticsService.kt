package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.exception.auth.UserNotFoundException
import kr.co.imoscloud.model.productionmanagement.*
import kr.co.imoscloud.repository.productionmanagement.ProductionPlanRepository
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipalOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 생산계획 관련 통계 및 분석 서비스
 * 레포트 화면에서 사용되는 분석 기능들을 담당
 */
@Service
class ProductionPlanAnalyticsService(
    private val productionPlanRepository: ProductionPlanRepository
) {
    private val log = LoggerFactory.getLogger(ProductionPlanAnalyticsService::class.java)

    /**
     * 계획 대비 실적 조회
     * 레포트 화면에서 계획대비 실적조회를 하기 위한 메서드
     */
    fun getPlanVsActualData(filter: PlanVsActualFilter): List<PlanVsActualGraphQLDto> {
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw UserNotFoundException()

        val materialIds = filter.systemMaterialIds?.filterNotNull()?.takeIf { it.isNotEmpty() }

        // 인터페이스 프로젝션 사용
        val results = productionPlanRepository.planVsActual(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            systemMaterialIds = materialIds,
            flagActive = true,
            startDate = filter.startDate,
            endDate = filter.endDate
        )
        
        // 인터페이스 프로젝션 결과를 GraphQL 응답용 DTO로 변환
        return results.map { it.toGraphQLResponse() }
    }

    /**
     * 기간별 생산 실적 조회
     * 레포트 화면에서 기간별 생산량 및 불량률 분석용
     */
    fun getPeriodicProduction(filter: PlanVsActualFilter): List<PeriodicProductionResponseDto> {
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw UserNotFoundException()

        val materialIds = filter.systemMaterialIds?.takeIf { it.isNotEmpty() }

        return productionPlanRepository.periodicProduction(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            systemMaterialIds = materialIds,
            flagActive = true,
            startDate = filter.startDate,
            endDate = filter.endDate
        )
    }
} 