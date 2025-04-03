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
        shiftType: String?,
        planStartDateFrom: LocalDate?,  // 계획시작일 범위 시작
        planStartDateTo: LocalDate?,    // 계획시작일 범위 끝
        flagActive: Boolean?
    ): List<ProductionPlan>
}