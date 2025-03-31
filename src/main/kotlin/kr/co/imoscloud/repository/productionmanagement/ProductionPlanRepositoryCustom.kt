package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import java.time.LocalDate

interface ProductionPlanRepositoryCustom {
    fun getProductionPlanList(
        site: String,
        compCd: String,
        prodPlanId: String?,
        orderId: String?,
        productId: String?,
        planStartDate: LocalDate?,
        planEndDate: LocalDate?,
        flagActive: Boolean?
    ): List<ProductionPlan>
}