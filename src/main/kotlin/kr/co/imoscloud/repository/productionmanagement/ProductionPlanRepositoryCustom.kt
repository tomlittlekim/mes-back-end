package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.model.productionmanagement.ProductionPlanDTO
import java.time.LocalDate

interface ProductionPlanRepositoryCustom {
    fun getProductionPlanList(
        site: String,
        compCd: String,
        prodPlanId: String?,
        orderId: String?,
        orderDetailId: String?,
        productId: String?,
        productName: String?,
        materialCategory: String?,
        shiftType: String?,
        planStartDateFrom: LocalDate?,  // 계획시작일 범위 시작
        planStartDateTo: LocalDate?,    // 계획시작일 범위 끝
        planEndDateFrom: LocalDate?,   // 추가: 계획종료일 범위 시작
        planEndDateTo: LocalDate?,     // 추가: 계획종료일 범위 끝
        flagActive: Boolean?
    ): List<ProductionPlanDTO>
}