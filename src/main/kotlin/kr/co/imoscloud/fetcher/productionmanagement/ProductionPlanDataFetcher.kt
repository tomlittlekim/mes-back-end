// ProductionPlanDataFetcher.kt
package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.model.productionmanagement.ProductionPlanFilter
import kr.co.imoscloud.model.productionmanagement.ProductionPlanInput
import kr.co.imoscloud.model.productionmanagement.ProductionPlanUpdate
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.security.UserPrincipal
import kr.co.imoscloud.service.productionmanagement.ProductionPlanService
import org.springframework.security.core.context.SecurityContextHolder

@DgsComponent
class ProductionPlanDataFetcher(
    private val productionPlanService: ProductionPlanService,
    private val workOrderRepository: WorkOrderRepository
) {
    // 생산계획 목록 조회
    @DgsQuery
    fun productionPlans(@InputArgument("filter") filter: ProductionPlanFilter): List<ProductionPlan> {
        return productionPlanService.getProductionPlans(filter)
    }

    // 생산계획 저장 (생성/수정)
    @DgsData(parentType = "Mutation", field = "saveProductionPlan")
    fun saveProductionPlan(
        @InputArgument("createdRows") createdRows: List<ProductionPlanInput>? = null,
        @InputArgument("updatedRows") updatedRows: List<ProductionPlanUpdate>? = null
    ): Boolean {
        return productionPlanService.saveProductionPlan(createdRows, updatedRows)
    }

    // 생산계획 삭제
    @DgsData(parentType = "Mutation", field = "deleteProductionPlan")
    fun deleteProductionPlan(
        @InputArgument("prodPlanId") prodPlanId: String
    ): Boolean {
        return productionPlanService.deleteProductionPlan(prodPlanId)
    }

    // 생산계획에 속한 작업지시 목록 조회 (GraphQL 리졸버)
    @DgsData(parentType = "ProductionPlan", field = "workOrders")
    fun workOrders(dfe: DgsDataFetchingEnvironment): List<WorkOrder> {
        val productionPlan = dfe.getSource<ProductionPlan>()
        val prodPlanId = productionPlan?.prodPlanId ?: return emptyList()

        val currentUser = getCurrentUserPrincipal()

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