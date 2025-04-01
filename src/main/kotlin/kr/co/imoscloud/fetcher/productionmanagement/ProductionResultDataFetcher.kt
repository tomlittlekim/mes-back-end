// ProductionResultDataFetcher.kt
package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.*
import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.model.productionmanagement.ProductionResultFilter
import kr.co.imoscloud.model.productionmanagement.ProductionResultInput
import kr.co.imoscloud.model.productionmanagement.ProductionResultUpdate
import kr.co.imoscloud.model.productionmanagement.WorkOrderFilter
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.service.productionmanagement.ProductionResultService
import kr.co.imoscloud.service.productionmanagement.WorkOrderService

@DgsComponent
class ProductionResultDataFetcher(
    private val productionResultService: ProductionResultService,
    private val workOrderService: WorkOrderService,
    private val workOrderRepository: WorkOrderRepository
) {
    // 특정 작업지시에 속한 생산실적 목록 조회
    @DgsQuery
    fun productionResultsByWorkOrderId(@InputArgument("workOrderId") workOrderId: String): List<ProductionResult> {
        return productionResultService.getProductionResultsByWorkOrderId(workOrderId)
    }

    // 조건에 맞는 생산실적 목록 조회
    @DgsQuery
    fun productionResults(@InputArgument("filter") filter: ProductionResultFilter): List<ProductionResult> {
        return productionResultService.getProductionResults(filter)
    }

    // 작업지시와 생산실적 통합 조회 (UI에서 필요한 경우)
    @DgsQuery
    fun workOrdersWithProductionResults(@InputArgument("filter") filter: WorkOrderFilter): List<WorkOrder> {
        // 단순히 작업지시 목록을 반환 (GraphQL 리졸버를 통해 생산실적을 채움)
        return workOrderService.getWorkOrders(filter)
    }

    // 생산실적 저장 (생성/수정)
    @DgsData(parentType = "Mutation", field = "saveProductionResult")
    fun saveProductionResult(
        @InputArgument("createdRows") createdRows: List<ProductionResultInput>? = null,
        @InputArgument("updatedRows") updatedRows: List<ProductionResultUpdate>? = null
    ): Boolean {
        return productionResultService.saveProductionResult(createdRows, updatedRows)
    }

    // 생산실적 삭제
    @DgsData(parentType = "Mutation", field = "deleteProductionResult")
    fun deleteProductionResult(
        @InputArgument("prodResultId") prodResultId: String
    ): Boolean {
        return productionResultService.deleteProductionResult(prodResultId)
    }

    // 생산실적에 연결된 작업지시 정보 조회 (GraphQL 리졸버)
    @DgsData(parentType = "ProductionResult", field = "workOrder")
    fun workOrder(dfe: DgsDataFetchingEnvironment): WorkOrder? {
        val productionResult = dfe.getSource<ProductionResult>()
        val workOrderId = productionResult?.workOrderId ?: return null

        return workOrderRepository.findByWorkOrderId(workOrderId)
    }
}