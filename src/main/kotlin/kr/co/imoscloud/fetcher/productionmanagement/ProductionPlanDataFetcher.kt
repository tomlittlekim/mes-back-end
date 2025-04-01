// ProductionPlanDataFetcher.kt
package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.*
import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.model.productionmanagement.ProductionPlanFilter
import kr.co.imoscloud.model.productionmanagement.ProductionPlanInput
import kr.co.imoscloud.model.productionmanagement.ProductionPlanUpdate
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.service.productionmanagement.ProductionPlanService
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal

@DgsComponent
class ProductionPlanDataFetcher(
    private val productionPlanService: ProductionPlanService,
    private val workOrderRepository: WorkOrderRepository
) {
    // 생산계획 목록 조회
    @DgsQuery
    fun productionPlans(@InputArgument("filter") filter: ProductionPlanFilter): List<ProductionPlan> {
        return productionPlanService.getProductionPlans(filter)
    }

    // 생산계획 저장 (생성/수정)
    @DgsData(parentType = "Mutation", field = "saveProductionPlan")
    fun saveProductionPlan(
        @InputArgument("createdRows") createdRows: List<ProductionPlanInput>? = null,
        @InputArgument("updatedRows") updatedRows: List<ProductionPlanUpdate>? = null
    ): Boolean {
        return productionPlanService.saveProductionPlan(createdRows, updatedRows)
    }

    // 생산계획 삭제
    @DgsData(parentType = "Mutation", field = "deleteProductionPlan")
    fun deleteProductionPlan(
        @InputArgument("prodPlanId") prodPlanId: String
    ): Boolean {
        return productionPlanService.deleteProductionPlan(prodPlanId)
    }

    // 생산계획에 속한 작업지시 목록 조회 (GraphQL 리졸버)
    @DgsData(parentType = "ProductionPlan", field = "workOrders")
    fun workOrders(dfe: DgsDataFetchingEnvironment): List<WorkOrder> {
        val productionPlan = dfe.getSource<ProductionPlan>()
        val prodPlanId = productionPlan?.prodPlanId ?: return emptyList()

        val currentUser = getCurrentUserPrincipal()

        return workOrderRepository.getWorkOrdersByProdPlanId(
            site = currentUser.getSite(),
            compCd = currentUser.getCompCd(),
            prodPlanId = prodPlanId
        )
    }

}