package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.service.productionmanagement.ProductionPlanResponseModel
import java.time.LocalDate

interface ProductionPlanRepositoryCustom {
    fun getProductionPlanList(
        site: String,
        compCd: String,
        prodPlanId: String?,
        orderId: String?,
        productId: String?,
        planStartDate: LocalDate?,
        planEndDate: LocalDate?
    ): List<ProductionPlanResponseModel?>
}