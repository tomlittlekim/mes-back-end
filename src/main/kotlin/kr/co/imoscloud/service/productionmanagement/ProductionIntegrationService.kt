package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.fetcher.productionmanagement.ProductionPlanFilter
import kr.co.imoscloud.fetcher.productionmanagement.WorkOrderInput
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.security.UserPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 생산계획과 작업지시를 통합적으로 관리하는 서비스
 */
@Service
class ProductionIntegrationService(
    private val productionPlanService: ProductionPlanService,
    private val workOrderService: WorkOrderService,
    private val workOrderRepository: WorkOrderRepository
) {
    /**
     * 생산계획 ID로 해당 계획에 속한 작업지시 목록을 조회합니다.
     */
    fun getWorkOrdersByProductionPlan(prodPlanId: String): List<WorkOrder> {
        return workOrderService.getWorkOrdersByProdPlanId(prodPlanId)
    }

    /**
     * 생산계획 목록과 각 계획에 연결된 작업지시 목록을 함께 조회합니다.
     * 이는 프론트엔드에서 한 번의 API 호출로 모든 데이터를 가져오기 위한 통합 메서드입니다.
     */
    fun getProductionPlansWithWorkOrders(filter: ProductionPlanFilter): List<ProductionPlan> {
        // 1. 생산계획 목록 조회
        val plans = productionPlanService.getProductionPlans(filter)

        // 2. 각 생산계획별 작업지시 데이터는 GraphQL 리졸버에서 처리하므로 여기서는 생산계획 목록만 반환
        return plans
    }

    /**
     * 생산계획에서 작업지시를 자동 생성합니다.
     * 생산계획의 정보를 바탕으로 새로운 작업지시를 생성합니다.
     */
    @Transactional
    fun createWorkOrderFromProductionPlan(prodPlanId: String, shiftType: String?, initialState: String = "PLANNED"): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()

            // 생산계획 조회
            val filter = ProductionPlanFilter(prodPlanId = prodPlanId)
            val plans = productionPlanService.getProductionPlans(filter)

            val plan = plans.firstOrNull() ?: return false

            // 작업지시 생성
            val workOrder = WorkOrderInput(
                prodPlanId = plan.prodPlanId,
                productId = plan.productId,
                orderQty = plan.planQty,
                shiftType = shiftType,
                state = initialState,
                flagActive = true
            )

            return workOrderService.saveWorkOrder(createdRows = listOf(workOrder))
        } catch (e: Exception) {
            println("Error in createWorkOrderFromProductionPlan: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    private fun getCurrentUserPrincipal(): UserPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication != null && authentication.isAuthenticated && authentication.principal is UserPrincipal) {
            return authentication.principal as UserPrincipal
        }

        throw SecurityException("현재 인증된 사용자 정보를 찾을 수 없습니다.")
    }
}