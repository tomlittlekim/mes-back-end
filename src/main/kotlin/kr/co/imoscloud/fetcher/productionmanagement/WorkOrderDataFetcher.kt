// WorkOrderDataFetcher.kt
package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.*
import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.model.productionmanagement.ProductionPlanFilter
import kr.co.imoscloud.model.productionmanagement.WorkOrderFilter
import kr.co.imoscloud.model.productionmanagement.WorkOrderInput
import kr.co.imoscloud.model.productionmanagement.WorkOrderUpdate
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.service.productionmanagement.ProductionPlanService
import kr.co.imoscloud.service.productionmanagement.WorkOrderService
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import org.slf4j.LoggerFactory

@DgsComponent
class WorkOrderDataFetcher(
    private val workOrderService: WorkOrderService,
    private val productionPlanService: ProductionPlanService,
    private val productionResultRepository: ProductionResultRepository
) {
    private val log = LoggerFactory.getLogger(WorkOrderDataFetcher::class.java)

    // 특정 생산계획에 속한 작업지시 목록 조회
    @DgsQuery
    fun workOrdersByProdPlanId(@InputArgument("prodPlanId") prodPlanId: String): List<WorkOrder> {
        try {
            return workOrderService.getWorkOrdersByProdPlanId(prodPlanId)
        } catch (e: Exception) {
            log.error("작업지시 목록 조회 중 오류 발생", e)
            return emptyList()
        }
    }

    // 조건에 맞는 작업지시 목록 조회
    @DgsQuery
    fun workOrders(@InputArgument("filter") filter: WorkOrderFilter): List<WorkOrder> {
        try {
            // flagActive가 설정되지 않은 경우 true로 설정하여 활성화된 데이터만 조회
            val activeFilter = filter.copy(flagActive = filter.flagActive ?: true)
            return workOrderService.getWorkOrders(activeFilter)
        } catch (e: Exception) {
            log.error("작업지시 목록 조회 중 오류 발생", e)
            return emptyList()
        }
    }

    // 작업지시 저장 (생성/수정)
    @DgsData(parentType = "Mutation", field = "saveWorkOrder")
    fun saveWorkOrder(
        @InputArgument("createdRows") createdRows: List<WorkOrderInput>? = null,
        @InputArgument("updatedRows") updatedRows: List<WorkOrderUpdate>? = null
    ): Boolean {
        try {
            return workOrderService.saveWorkOrder(createdRows, updatedRows)
        } catch (e: Exception) {
            log.error("작업지시 저장 중 오류 발생", e)
            return false
        }
    }

    // 작업지시 삭제 (소프트 삭제로 변경)
    @DgsData(parentType = "Mutation", field = "deleteWorkOrder")
    fun deleteWorkOrder(
        @InputArgument("workOrderId") workOrderId: String
    ): Boolean {
        try {
            return workOrderService.softDeleteWorkOrder(workOrderId)
        } catch (e: Exception) {
            log.error("작업지시 삭제 중 오류 발생", e)
            return false
        }
    }

    // 작업 시작
    @DgsData(parentType = "Mutation", field = "startWorkOrder")
    fun startWorkOrder(
        @InputArgument("workOrderId") workOrderId: String
    ): Boolean {
        try {
            return workOrderService.startWorkOrder(workOrderId)
        } catch (e: Exception) {
            log.error("작업 시작 중 오류 발생", e)
            return false
        }
    }

    // 작업 완료
    @DgsData(parentType = "Mutation", field = "completeWorkOrder")
    fun completeWorkOrder(
        @InputArgument("workOrderId") workOrderId: String
    ): Boolean {
        try {
            return workOrderService.completeWorkOrder(workOrderId)
        } catch (e: Exception) {
            log.error("작업 완료 중 오류 발생", e)
            return false
        }
    }

    // 작업지시에 연결된 생산계획 정보 조회 (GraphQL 리졸버)
    @DgsData(parentType = "WorkOrder", field = "productionPlan")
    fun productionPlan(dfe: DgsDataFetchingEnvironment): ProductionPlan? {
        try {
            val workOrder = dfe.getSource<WorkOrder>()
            val prodPlanId = workOrder?.prodPlanId ?: return null

            val filter = ProductionPlanFilter(prodPlanId = prodPlanId, flagActive = true)
            val plans = productionPlanService.getProductionPlans(filter)

            return plans.firstOrNull()
        } catch (e: Exception) {
            log.error("생산계획 조회 중 오류 발생", e)
            return null
        }
    }

    // 작업지시에 연결된 생산실적 목록 조회 (GraphQL 리졸버)
    @DgsData(parentType = "WorkOrder", field = "productionResults")
    fun productionResults(dfe: DgsDataFetchingEnvironment): List<ProductionResult> {
        try {
            val workOrder = dfe.getSource<WorkOrder>()
            val workOrderId = workOrder?.workOrderId ?: return emptyList()

            val currentUser = getCurrentUserPrincipal()

            return productionResultRepository.getProductionResultsByWorkOrderId(
                site = currentUser.getSite(),
                compCd = currentUser.compCd,
                workOrderId = workOrderId
            )
        } catch (e: Exception) {
            log.error("생산실적 목록 조회 중 오류 발생", e)
            return emptyList()
        }
    }
}