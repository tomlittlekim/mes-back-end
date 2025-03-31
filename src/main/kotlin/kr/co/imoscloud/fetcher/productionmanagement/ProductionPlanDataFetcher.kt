package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.service.productionmanagement.ProductionPlanService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@DgsComponent
class ProductionPlanDataFetcher(
    val productionPlanService: ProductionPlanService
) {

    @DgsQuery
    fun productionPlans(@InputArgument("filter") filter: ProductionPlanFilter): List<ProductionPlan> {
        return productionPlanService.getProductionPlans(
            ProductionPlanFilter(
                prodPlanId = filter.prodPlanId,
                orderId = filter.orderId,
                productId = filter.productId,
                planStartDate = filter.planStartDate,
                planEndDate = filter.planEndDate,
                flagActive = filter.flagActive
            )
        )
    }

    @DgsData(parentType = "Mutation", field = "saveProductionPlan")
    fun saveProductionPlan(
        @InputArgument("createdRows") createdRows: List<ProductionPlanInput>? = null,
        @InputArgument("updatedRows") updatedRows: List<ProductionPlanUpdate>? = null
    ): Boolean {
        return productionPlanService.saveProductionPlan(createdRows, updatedRows)
    }

    @DgsData(parentType = "Mutation", field = "deleteProductionPlan")
    fun deleteProductionPlan(
        @InputArgument("prodPlanId") prodPlanId: String
    ): Boolean {
        return productionPlanService.deleteProductionPlan(prodPlanId)
    }
}

data class ProductionPlanFilter(
    var prodPlanId: String? = null,
    var orderId: String? = null,
    var productId: String? = null,
    var planStartDate: LocalDate? = null,
    var planEndDate: LocalDate? = null,
    var flagActive: Boolean? = null
)

data class ProductionPlanInput(
    val orderId: String? = null,
    val productId: String? = null,
    val planQty: Double? = null,
    val planStartDate: String? = null,
    val planEndDate: String? = null,
    val flagActive: Boolean? = true
) {
    fun toLocalDateTimes(): Pair<LocalDateTime?, LocalDateTime?> {
        val startDate = planStartDate?.let {
            val date = LocalDate.parse(it)
            LocalDateTime.of(date, LocalTime.MIN)
        }

        val endDate = planEndDate?.let {
            val date = LocalDate.parse(it)
            LocalDateTime.of(date, LocalTime.MAX)
        }

        return Pair(startDate, endDate)
    }
}

data class ProductionPlanUpdate(
    val prodPlanId: String,
    val orderId: String? = null,
    val productId: String? = null,
    val planQty: Double? = null,
    val planStartDate: String? = null,
    val planEndDate: String? = null,
    val flagActive: Boolean? = null
) {
    fun toLocalDateTimes(): Pair<LocalDateTime?, LocalDateTime?> {
        val startDate = planStartDate?.let {
            val date = LocalDate.parse(it)
            LocalDateTime.of(date, LocalTime.MIN)
        }

        val endDate = planEndDate?.let {
            val date = LocalDate.parse(it)
            LocalDateTime.of(date, LocalTime.MAX)
        }

        return Pair(startDate, endDate)
    }
}