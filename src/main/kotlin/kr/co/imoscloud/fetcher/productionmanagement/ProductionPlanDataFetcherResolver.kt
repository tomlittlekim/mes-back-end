package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.security.UserPrincipal
import org.springframework.security.core.context.SecurityContextHolder

@DgsComponent
class ProductionPlanDataFetcherResolver(
    private val workOrderRepository: WorkOrderRepository
) {
    @DgsData(parentType = "ProductionPlan", field = "workOrders")
    fun workOrders(dfe: DgsDataFetchingEnvironment): List<WorkOrder> {
        val productionPlan = dfe.getSource<ProductionPlan>()
        // 안전 호출 연산자 ?. 사용
        val prodPlanId = productionPlan?.prodPlanId ?: return emptyList()

        val currentUser = getCurrentUserPrincipal()

        // 생산계획 ID에 해당하는 작업지시 목록 조회
        return workOrderRepository.getWorkOrdersByProdPlanId(
            site = currentUser.getSite(),
            compCd = currentUser.getCompCd(),
            prodPlanId = prodPlanId
        )
    }

    private fun getCurrentUserPrincipal(): UserPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication != null && authentication.isAuthenticated && authentication.principal is UserPrincipal) {
            return authentication.principal as UserPrincipal
        }

        throw SecurityException("현재 인증된 사용자 정보를 찾을 수 없습니다.")
    }
}