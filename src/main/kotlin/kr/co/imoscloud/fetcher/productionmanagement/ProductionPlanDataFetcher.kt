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

@DgsComponent
class ProductionPlanDataFetcher(
    private val productionPlanService: ProductionPlanService,
    private val workOrderRepository: WorkOrderRepository
) {
    private val log = LoggerFactory.getLogger(ProductionPlanDataFetcher::class.java)

    // 생산계획 목록 조회
    @DgsQuery
    fun productionPlans(@InputArgument("filter") filter: ProductionPlanFilter): List<ProductionPlan> {
        try {
            return productionPlanService.getProductionPlans(filter)
        } catch (e: SecurityException) {
            log.error("인증 오류: {}", e.message)
            return emptyList()
        } catch (e: Exception) {
            log.error("생산계획 목록 조회 중 오류 발생", e)
            return emptyList()
        }
    }

    // 생산계획 저장 (생성/수정)
    @DgsData(parentType = "Mutation", field = "saveProductionPlan")
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

    // 생산계획 삭제
    @DgsData(parentType = "Mutation", field = "deleteProductionPlan")
    fun deleteProductionPlan(
        @InputArgument("prodPlanId") prodPlanId: String
    ): Boolean {
        try {
            return productionPlanService.deleteProductionPlan(prodPlanId)
        } catch (e: SecurityException) {
            log.error("인증 오류: {}", e.message)
            return false
        } catch (e: Exception) {
            log.error("생산계획 삭제 중 오류 발생", e)
            return false
        }
    }

    // 생산계획에 속한 작업지시 목록 조회 (GraphQL 리졸버)
    @DgsData(parentType = "ProductionPlan", field = "workOrders")
    fun workOrders(dfe: DgsDataFetchingEnvironment): List<WorkOrder> {
        val productionPlan = dfe.getSource<ProductionPlan>()
        val prodPlanId = productionPlan?.prodPlanId ?: return emptyList()

        try {
            // 사용자 정보 가져오기
            val currentUser = SecurityUtils.getCurrentUserPrincipalOrNull()
                ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

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
}