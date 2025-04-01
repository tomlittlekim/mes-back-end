package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.service.productionmanagement.WorkOrderService

@DgsComponent
class WorkOrderDataFetcher(
    val workOrderService: WorkOrderService
) {

    @DgsQuery
    fun workOrdersByProdPlanId(@InputArgument("prodPlanId") prodPlanId: String): List<WorkOrder> {
        return workOrderService.getWorkOrdersByProdPlanId(prodPlanId)
    }

    @DgsQuery
    fun workOrders(@InputArgument("filter") filter: WorkOrderFilter): List<WorkOrder> {
        return workOrderService.getWorkOrders(
            WorkOrderFilter(
                workOrderId = filter.workOrderId,
                prodPlanId = filter.prodPlanId,
                productId = filter.productId,
                shiftType = filter.shiftType,
                state = filter.state,
                flagActive = filter.flagActive
            )
        )
    }

    @DgsData(parentType = "Mutation", field = "saveWorkOrder")
    fun saveWorkOrder(
        @InputArgument("createdRows") createdRows: List<WorkOrderInput>? = null,
        @InputArgument("updatedRows") updatedRows: List<WorkOrderUpdate>? = null
    ): Boolean {
        return workOrderService.saveWorkOrder(createdRows, updatedRows)
    }

    @DgsData(parentType = "Mutation", field = "deleteWorkOrder")
    fun deleteWorkOrder(
        @InputArgument("workOrderId") workOrderId: String
    ): Boolean {
        return workOrderService.deleteWorkOrder(workOrderId)
    }
}

data class WorkOrderFilter(
    var workOrderId: String? = null,
    var prodPlanId: String? = null,
    var productId: String? = null,
    var shiftType: String? = null,
    var state: String? = null,
    var flagActive: Boolean? = null
)

data class WorkOrderInput(
    val prodPlanId: String? = null,
    val productId: String? = null,
    val orderQty: Double? = null,
    val shiftType: String? = null,
    val state: String? = null,
    val flagActive: Boolean? = true
)

data class WorkOrderUpdate(
    val workOrderId: String,
    val prodPlanId: String? = null,
    val productId: String? = null,
    val orderQty: Double? = null,
    val shiftType: String? = null,
    val state: String? = null,
    val flagActive: Boolean? = null
)