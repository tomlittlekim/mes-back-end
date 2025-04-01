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

@DgsComponent
class WorkOrderDataFetcher(
    private val workOrderService: WorkOrderService,
    private val productionPlanService: ProductionPlanService,
    private val productionResultRepository: ProductionResultRepository
) {
    // 특정 생산계획에 속한 작업지시 목록 조회
    @DgsQuery
    fun workOrdersByProdPlanId(@InputArgument("prodPlanId") prodPlanId: String): List<WorkOrder> {
        return workOrderService.getWorkOrdersByProdPlanId(prodPlanId)
    }

    // 조건에 맞는 작업지시 목록 조회
    @DgsQuery
    fun workOrders(@InputArgument("filter") filter: WorkOrderFilter): List<WorkOrder> {
        return workOrderService.getWorkOrders(filter)
    }

    // 작업지시 저장 (생성/수정)
    @DgsData(parentType = "Mutation", field = "saveWorkOrder")
    fun saveWorkOrder(
        @InputArgument("createdRows") createdRows: List<WorkOrderInput>? = null,
        @InputArgument("updatedRows") updatedRows: List<WorkOrderUpdate>? = null
    ): Boolean {
        return workOrderService.saveWorkOrder(createdRows, updatedRows)
    }

    // 작업지시 삭제
    @DgsData(parentType = "Mutation", field = "deleteWorkOrder")
    fun deleteWorkOrder(
        @InputArgument("workOrderId") workOrderId: String
    ): Boolean {
        return workOrderService.deleteWorkOrder(workOrderId)
    }

    // 작업지시에 연결된 생산계획 정보 조회 (GraphQL 리졸버)
    @DgsData(parentType = "WorkOrder", field = "productionPlan")
    fun productionPlan(dfe: DgsDataFetchingEnvironment): ProductionPlan? {
        val workOrder = dfe.getSource<WorkOrder>()
        val prodPlanId = workOrder?.prodPlanId ?: return null

        val filter = ProductionPlanFilter(prodPlanId = prodPlanId)
        val plans = productionPlanService.getProductionPlans(filter)

        return plans.firstOrNull()
    }

    // 작업지시에 연결된 생산실적 목록 조회 (GraphQL 리졸버)
    @DgsData(parentType = "WorkOrder", field = "productionResults")
    fun productionResults(dfe: DgsDataFetchingEnvironment): List<ProductionResult> {
        val workOrder = dfe.getSource<WorkOrder>()
        val workOrderId = workOrder?.workOrderId ?: return emptyList()

        val currentUser = getCurrentUserPrincipal()


        return productionResultRepository.getProductionResultsByWorkOrderId(
            site = currentUser.getSite(),
            compCd = currentUser.getCompCd(),
            workOrderId = workOrderId
        )
    }

}