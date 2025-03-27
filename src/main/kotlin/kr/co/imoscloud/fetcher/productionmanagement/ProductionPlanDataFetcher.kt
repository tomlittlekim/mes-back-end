package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.service.productionmanagement.ProductionPlanResponseModel
import kr.co.imoscloud.service.productionmanagement.ProductionPlanService
import java.time.LocalDate

@DgsComponent
class ProductionPlanDataFetcher(
    val productionPlanService: ProductionPlanService
) {
    @DgsQuery
    fun productionPlans(@InputArgument("filter") filter: ProductionPlanFilter): List<ProductionPlanResponseModel?> {

        return productionPlanService.getProductionPlans(
            ProductionPlanFilter(
                prodPlanId = filter.prodPlanId,
                orderId = filter.orderId,
                productId = filter.productId,
                planStartDate = filter.planStartDate,
                planEndDate = filter.planEndDate
            )
        )
    }
}

data class ProductionPlanFilter(
    var prodPlanId: String? = null,
    var orderId: String? = null,
    var productId: String? = null,
    var planStartDate: LocalDate? = null,
    var planEndDate: LocalDate? = null
)