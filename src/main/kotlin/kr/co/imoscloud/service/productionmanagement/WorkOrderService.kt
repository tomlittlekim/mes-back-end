package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.model.productionmanagement.WorkOrderFilter
import kr.co.imoscloud.model.productionmanagement.WorkOrderInput
import kr.co.imoscloud.model.productionmanagement.WorkOrderUpdate
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import org.springframework.stereotype.Service

@Service
class WorkOrderService(
    val workOrderRepository: WorkOrderRepository
) {
    fun getWorkOrdersByProdPlanId(prodPlanId: String): List<WorkOrder> {
        val currentUser = getCurrentUserPrincipal()
        return workOrderRepository.getWorkOrdersByProdPlanId(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            prodPlanId = prodPlanId
        )
    }

    fun getWorkOrders(filter: WorkOrderFilter): List<WorkOrder> {
        return workOrderRepository.getWorkOrderList(
            site = "imos",
            compCd = "8pin",
            workOrderId = filter.workOrderId,
            prodPlanId = filter.prodPlanId,
            productId = filter.productId,
            shiftType = filter.shiftType,
            state = filter.state,
            flagActive = filter.flagActive
        )
    }

    fun saveWorkOrder(
        createdRows: List<WorkOrderInput>? = null,
        updatedRows: List<WorkOrderUpdate>? = null
    ): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()

            // 새로운 작업지시 저장
            createdRows?.forEach { input ->
                val newWorkOrder = WorkOrder().apply {
                    site = currentUser.getSite()
                    compCd = currentUser.compCd
                    workOrderId = "WO" + System.currentTimeMillis() // 임시 ID 생성 방식
                    prodPlanId = input.prodPlanId
                    productId = input.productId
                    orderQty = input.orderQty
                    shiftType = input.shiftType
                    state = input.state ?: "PLANNED" // 기본값 설정
                    flagActive = input.flagActive ?: true
                    createCommonCol(currentUser)
                }

                workOrderRepository.save(newWorkOrder)
            }

            // 기존 작업지시 업데이트
            updatedRows?.forEach { update ->
                val existingWorkOrder = workOrderRepository.findByWorkOrderId(update.workOrderId)

                existingWorkOrder?.let { workOrder ->
                    workOrder.apply {
                        update.prodPlanId?.let { prodPlanId = it }
                        update.productId?.let { productId = it }
                        update.orderQty?.let { orderQty = it }
                        update.shiftType?.let { shiftType = it }
                        update.state?.let { state = it }
                        update.flagActive?.let { flagActive = it }
                        updateCommonCol(currentUser)
                    }

                    workOrderRepository.save(workOrder)
                }
            }

            return true
        } catch (e: Exception) {
            // 로깅 추가
            println("Error in saveWorkOrder: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun deleteWorkOrder(workOrderId: String): Boolean {
        try {
            val existingWorkOrder = workOrderRepository.findByWorkOrderId(workOrderId)

            existingWorkOrder?.let {
                workOrderRepository.delete(it)
                return true
            }

            return false
        } catch (e: Exception) {
            // 로깅 추가
            println("Error in deleteWorkOrder: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

}