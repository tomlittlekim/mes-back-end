package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.service.productionmanagement.ProductionPlanService

@DgsComponent
class WorkOrderDataFetcherResolver(
    private val productionPlanService: ProductionPlanService
) {
    @DgsData(parentType = "WorkOrder", field = "productionPlan")
    fun productionPlan(dfe: DgsDataFetchingEnvironment): ProductionPlan? {
        val workOrder = dfe.getSource<WorkOrder>()
        val prodPlanId = workOrder.prodPlanId ?: return null

        // 생산계획 ID로 생산계획 정보 조회
        val filter = ProductionPlanFilter(prodPlanId = prodPlanId)
        val plans = productionPlanService.getProductionPlans(filter)

        return plans.firstOrNull()
    }
}