package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import java.time.LocalDateTime

interface WorkOrderRepositoryCustom {
    fun getWorkOrderList(
        site: String,
        compCd: String,
        workOrderId: String?,
        prodPlanId: String?,
        productId: String?,
        shiftType: String?,
        state: List<String>?,
        flagActive: Boolean?,
        planStartDateFrom: LocalDateTime?,
        planStartDateTo: LocalDateTime?,
    ): List<WorkOrder>

    fun getWorkOrdersByProdPlanId(
        site: String,
        compCd: String,
        prodPlanId: String
    ): List<WorkOrder>
}