package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.fetcher.productionmanagement.ProductionPlanFilter
import kr.co.imoscloud.repository.productionmanagement.ProductionPlanRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ProductionPlanService(
    val productionPlanRepository: ProductionPlanRepository
) {
    fun getProductionPlans(filter: ProductionPlanFilter): List<ProductionPlanResponseModel?> {
        val result = productionPlanRepository.getProductionPlanList(
            site = "imos",
            compCd = "epin",
            prodPlanId = filter.prodPlanId,
            orderId = filter.orderId,
            productId = filter.productId,
            planStartDate = filter.planStartDate,
            planEndDate = filter.planEndDate
        )

        return result
    }
}

data class ProductionPlanResponseModel(
    val prodPlanId: String? = null,
    val orderId: String? = null,
    val productId: String? = null,
    val planQty: Double? = null,
    val planStartDate: LocalDateTime? = null,
    val planEndDate: LocalDateTime? = null
)