package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.*
import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.model.productionmanagement.ProductionPlanFilter
import kr.co.imoscloud.model.productionmanagement.ProductionPlanInput
import kr.co.imoscloud.model.productionmanagement.ProductionPlanUpdate
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.service.productionmanagement.ProductionPlanService
import kr.co.imoscloud.util.SecurityUtils
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder

@DgsComponent
class ProductionPlanDataFetcher(
    private val productionPlanService: ProductionPlanService,
    private val workOrderRepository: WorkOrderRepository
) {
    private val log = LoggerFactory.getLogger(ProductionPlanDataFetcher::class.java)

    // 생산계획 목록 조회
    @DgsQuery
    fun productionPlans(@InputArgument("filter") filter: ProductionPlanFilter): List<ProductionPlan> {
        // 현재 사용자 정보 가져오기
        val currentUser = SecurityUtils.getCurrentUserPrincipalOrNull()

        // 서비스에 사용자 정보 전달
        return productionPlanService.getProductionPlans(filter, currentUser)
    }

    // 생산계획 저장 (생성/수정)
    @DgsData(parentType = "Mutation", field = "saveProductionPlan")
    fun saveProductionPlan(
        @InputArgument("createdRows") createdRows: List<ProductionPlanInput>? = null,
        @InputArgument("updatedRows") updatedRows: List<ProductionPlanUpdate>? = null
    ): Boolean {
        try {
            // 인증 정보 확인 및 로깅
            val auth = SecurityContextHolder.getContext().authentication
            log.debug("DataFetcher에서 인증 정보 확인: {}", auth != null)

            // 현재 사용자 정보 가져오기
            val currentUser = SecurityUtils.getCurrentUserPrincipalOrNull()

            // 서비스에 사용자 정보 전달
            return productionPlanService.saveProductionPlan(
                createdRows = createdRows,
                updatedRows = updatedRows,
                userPrincipal = currentUser  // 명시적으로 사용자 정보 전달
            )
        } catch (e: Exception) {
            log.error("DataFetcher에서 오류 발생", e)
            return false
        }
    }

    // 생산계획 삭제
    @DgsData(parentType = "Mutation", field = "deleteProductionPlan")
    fun deleteProductionPlan(
        @InputArgument("prodPlanId") prodPlanId: String
    ): Boolean {
        // 현재 사용자 정보 가져오기
        val currentUser = SecurityUtils.getCurrentUserPrincipalOrNull()

        // 서비스에 사용자 정보 전달
        return productionPlanService.deleteProductionPlan(prodPlanId, currentUser)
    }

    // 생산계획에 속한 작업지시 목록 조회 (GraphQL 리졸버)
    @DgsData(parentType = "ProductionPlan", field = "workOrders")
    fun workOrders(dfe: DgsDataFetchingEnvironment): List<WorkOrder> {
        val productionPlan = dfe.getSource<ProductionPlan>()
        val prodPlanId = productionPlan?.prodPlanId ?: return emptyList()

        // 기본값 사용
        val site = "imos"
        val compCd = "8pin"

        try {
            // 사용자 정보 가져오기 시도
            val currentUser = SecurityUtils.getCurrentUserPrincipalOrNull()

            return workOrderRepository.getWorkOrdersByProdPlanId(
                site = currentUser?.getSite() ?: site,
                compCd = currentUser?.compCd ?: compCd,
                prodPlanId = prodPlanId
            )
        } catch (e: Exception) {
            // 오류 발생 시 기본값 사용
            log.warn("workOrders 조회 중 오류 발생, 기본값 사용: {}", e.message)
            return workOrderRepository.getWorkOrdersByProdPlanId(
                site = site,
                compCd = compCd,
                prodPlanId = prodPlanId
            )
        }
    }
}