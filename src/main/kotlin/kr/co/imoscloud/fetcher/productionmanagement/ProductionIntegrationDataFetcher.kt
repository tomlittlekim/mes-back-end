package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.service.productionmanagement.ProductionIntegrationService

@DgsComponent
class ProductionIntegrationDataFetcher(
    private val productionIntegrationService: ProductionIntegrationService
) {
    @DgsQuery
    fun productionPlansWithWorkOrders(@InputArgument("filter") filter: ProductionPlanFilter): List<ProductionPlan> {
        return productionIntegrationService.getProductionPlansWithWorkOrders(filter)
    }

    @DgsData(parentType = "Mutation", field = "createWorkOrderFromProductionPlan")
    fun createWorkOrderFromProductionPlan(
        @InputArgument("prodPlanId") prodPlanId: String,
        @InputArgument("shiftType") shiftType: String?,
        @InputArgument("initialState") initialState: String?
    ): Boolean {
        return productionIntegrationService.createWorkOrderFromProductionPlan(
            prodPlanId = prodPlanId,
            shiftType = shiftType,
            initialState = initialState ?: "PLANNED"
        )
    }
}