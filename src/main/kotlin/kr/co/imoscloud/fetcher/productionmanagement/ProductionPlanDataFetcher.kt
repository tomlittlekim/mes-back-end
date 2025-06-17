package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.*
import kr.co.imoscloud.entity.material.MaterialMaster
import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.exception.auth.UserNotFoundException
import kr.co.imoscloud.model.productionmanagement.*
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.service.productionmanagement.ProductionPlanService
import kr.co.imoscloud.service.productionmanagement.ProductionPlanAnalyticsService
import kr.co.imoscloud.util.DateUtils
import kr.co.imoscloud.util.SecurityUtils
import org.slf4j.LoggerFactory

@DgsComponent
class ProductionPlanDataFetcher(
    private val productionPlanService: ProductionPlanService,
    private val productionPlanAnalyticsService: ProductionPlanAnalyticsService,
    private val workOrderRepository: WorkOrderRepository,
) {
    private val log = LoggerFactory.getLogger(ProductionPlanDataFetcher::class.java)

    // 생산계획 목록 조회 - ProductionPlanFilter DTO를 직접 사용
    @DgsQuery
    fun productionPlans(@InputArgument("filter") filter: ProductionPlanFilter?): List<ProductionPlanDTO> {
        try {
            // 필터가 null인 경우 기본 필터 생성
            val productionPlanFilter = filter ?: ProductionPlanFilter(flagActive = true)

            // DTO를 직접 반환
            return productionPlanService.getProductionPlans(productionPlanFilter)
        } catch (e: SecurityException) {
            log.error("인증 오류: {}", e.message)
            return emptyList()
        } catch (e: Exception) {
            log.error("생산계획 목록 조회 중 오류 발생", e)
            return emptyList()
        }
    }

    // 제품 목록 조회 메서드 유지 (메뉴 마운트 시 한번에 로드하는 용도)
    @DgsQuery
    fun productMaterials(): List<MaterialMaster?> {
        try {
            return productionPlanService.getProductMaterials()
        } catch (e: Exception) {
            log.error("제품 정보 조회 중 오류 발생", e)
            return emptyList()
        }
    }

    // 생산계획 저장 (생성/수정)
    @DgsMutation
    fun saveProductionPlan(
        @InputArgument("createdRows") createdRows: List<ProductionPlanInput>? = null,
        @InputArgument("updatedRows") updatedRows: List<ProductionPlanUpdate>? = null
    ): Boolean {
        try {
            return productionPlanService.saveProductionPlan(
                createdRows = createdRows,
                updatedRows = updatedRows
            )
        } catch (e: SecurityException) {
            log.error("인증 오류: {}", e.message)
            return false
        } catch (e: Exception) {
            log.error("생산계획 저장 중 오류 발생", e)
            return false
        }
    }

    // 생산계획 삭제 (소프트 삭제로 변경)
    @DgsMutation
    fun deleteProductionPlans(
        @InputArgument("prodPlanIds") prodPlanIds: List<String>
    ): ProductionPlanDeleteResult {
        try {
            return productionPlanService.softDeleteProductionPlans(prodPlanIds)
        } catch (e: SecurityException) {
            log.error("인증 오류: {}", e.message)
            return ProductionPlanDeleteResult(
                success = false,
                totalRequested = prodPlanIds.size,
                deletedCount = 0,
                skippedCount = 0,
                skippedPlans = emptyList(),
                message = "인증 오류가 발생했습니다: ${e.message}"
            )
        } catch (e: Exception) {
            log.error("생산계획 삭제 중 오류 발생", e)
            return ProductionPlanDeleteResult(
                success = false,
                totalRequested = prodPlanIds.size,
                deletedCount = 0,
                skippedCount = 0,
                skippedPlans = emptyList(),
                message = "삭제 중 오류가 발생했습니다: ${e.message}"
            )
        }
    }

    // 생산계획에 속한 작업지시 목록 조회 (GraphQL 리졸버)
    @DgsData(parentType = "ProductionPlan", field = "workOrders")
    fun workOrders(dfe: DgsDataFetchingEnvironment): List<WorkOrder> {
        val productionPlan = dfe.getSource<ProductionPlanDTO>()
        val prodPlanId = productionPlan?.prodPlanId ?: return emptyList()

        try {
            // 사용자 정보 가져오기
            val currentUser = SecurityUtils.getCurrentUserPrincipalOrNull()
                ?: throw UserNotFoundException()

            return workOrderRepository.getWorkOrdersByProdPlanId(
                site = currentUser.getSite(),
                compCd = currentUser.compCd,
                prodPlanId = prodPlanId
            )
        } catch (e: SecurityException) {
            log.error("인증 오류: {}", e.message)
            return emptyList()
        } catch (e: Exception) {
            log.error("작업지시 목록 조회 중 오류 발생", e)
            return emptyList()
        }
    }

    // === 통계/분석 관련 메서드들 (분리된 서비스 사용) ===

    @DgsQuery
    fun planVsActual(@InputArgument("filter") filterInput: PlanVsActualFilter): List<PlanVsActualGraphQLDto> {
        return productionPlanAnalyticsService.getPlanVsActualData(filterInput)
    }

    // planVsActual과 같은 필터 사용
    @DgsQuery
    fun periodicProduction(@InputArgument("filter") filterInput: PlanVsActualFilter): List<PeriodicProductionResponseDto> {
        return productionPlanAnalyticsService.getPeriodicProduction(filterInput)
    }

    @DgsQuery
    fun getDefectInfo(@InputArgument("productId") productId: String?): List<defectInfoResponse> {
        return productionPlanAnalyticsService.getDefectInfo(productId)
    }
}