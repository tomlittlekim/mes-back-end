package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.model.productionmanagement.WorkOrderFilter
import kr.co.imoscloud.model.productionmanagement.WorkOrderInput
import kr.co.imoscloud.model.productionmanagement.WorkOrderUpdate
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.util.DateUtils.getSearchDateRange
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class WorkOrderService(
    val workOrderRepository: WorkOrderRepository
) {
    private val log = LoggerFactory.getLogger(WorkOrderService::class.java)

    fun getWorkOrdersByProdPlanId(prodPlanId: String): List<WorkOrder> {
        val currentUser = getCurrentUserPrincipal()
        // 활성화된 작업지시만 조회
        return workOrderRepository.getWorkOrdersByProdPlanId(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            prodPlanId = prodPlanId
        )
    }

    fun getWorkOrders(filter: WorkOrderFilter): List<WorkOrder> {
        val currentUser = getCurrentUserPrincipal()
        // flagActive가 명시적으로 설정되지 않은 경우 true로 설정하여 활성화된 데이터만 조회
        val activeFilter = filter.copy(flagActive = filter.flagActive ?: true)
        val (fromDate , toDate) = getSearchDateRange(filter.planStartDateFrom,filter.planStartDateTo)

        return workOrderRepository.getWorkOrderList(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            workOrderId = activeFilter.workOrderId,
            prodPlanId = activeFilter.prodPlanId,
            productId = activeFilter.productId,
            shiftType = activeFilter.shiftType,
            state = activeFilter.state,
            flagActive = activeFilter.flagActive,
            planStartDateFrom = fromDate,
            planStartDateTo = toDate,
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
                    flagActive = true // 항상 활성 상태로 생성
                    createCommonCol(currentUser)
                }

                workOrderRepository.save(newWorkOrder)
            }

            // 기존 작업지시 업데이트
            updatedRows?.forEach { update ->
                try {
                    // 특정 조건에 맞는 작업지시를 직접 찾는 쿼리 사용
                    val existingWorkOrder = workOrderRepository.findBySiteAndCompCdAndWorkOrderId(
                        currentUser.getSite(),
                        currentUser.compCd,
                        update.workOrderId
                    )

                    existingWorkOrder?.let { workOrder ->
                        workOrder.apply {
                            update.prodPlanId?.let { prodPlanId = it }
                            update.productId?.let { productId = it }
                            update.orderQty?.let { orderQty = it }
                            update.shiftType?.let { shiftType = it }
                            update.state?.let { state = it }
                            // flagActive는 업데이트하지 않음 (사용자가 수정할 수 없음)
                            updateCommonCol(currentUser)
                        }

                        workOrderRepository.save(workOrder)
                    } ?: log.warn("업데이트할 작업지시를 찾을 수 없습니다: {}", update.workOrderId)
                } catch (e: Exception) {
                    log.error("작업지시 업데이트 중 오류 발생: {}", update.workOrderId, e)
                }
            }

            return true
        } catch (e: Exception) {
            log.error("작업지시 저장 중 오류 발생", e)
            return false
        }
    }

    /**
     * 작업 상태 변경 메서드 - 작업 시작 또는 완료 처리를 위한 메서드
     *
     * @param workOrderId 작업지시 ID
     * @param newState 변경할 상태 (IN_PROGRESS, COMPLETED 등)
     * @return 상태 변경 성공 여부
     */
    private fun updateWorkOrderState(workOrderId: String, newState: String): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()

            // 특정 조건에 맞는 작업지시를 직접 찾는 쿼리 사용
            val existingWorkOrder = workOrderRepository.findBySiteAndCompCdAndWorkOrderId(
                currentUser.getSite(),
                currentUser.compCd,
                workOrderId
            )

            existingWorkOrder?.let { workOrder ->
                workOrder.apply {
                    state = newState
                    updateCommonCol(currentUser)
                }

                workOrderRepository.save(workOrder)
                return true
            }

            log.warn("상태를 변경할 작업지시를 찾을 수 없습니다: {}", workOrderId)
            return false
        } catch (e: Exception) {
            log.error("작업지시 상태 변경 중 오류 발생", e)
            return false
        }
    }

    /**
     * 작업 시작 메서드
     *
     * @param workOrderId 작업지시 ID
     * @return 작업 시작 성공 여부
     */
    fun startWorkOrder(workOrderId: String): Boolean {
        return updateWorkOrderState(workOrderId, "IN_PROGRESS")
    }

    /**
     * 작업 완료 메서드
     *
     * @param workOrderId 작업지시 ID
     * @return 작업 완료 성공 여부
     */
    fun completeWorkOrder(workOrderId: String): Boolean {
        return updateWorkOrderState(workOrderId, "COMPLETED")
    }

    /**
     * 작업지시를 소프트 삭제하는 메서드 (flagActive = false로 설정)
     */
    fun softDeleteWorkOrder(workOrderId: String): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()

            // 특정 조건에 맞는 작업지시를 직접 찾는 쿼리 사용
            val existingWorkOrder = workOrderRepository.findBySiteAndCompCdAndWorkOrderId(
                currentUser.getSite(),
                currentUser.compCd,
                workOrderId
            )

            existingWorkOrder?.let {
                // flagActive를 false로 설정
                it.flagActive = false
                it.updateCommonCol(currentUser)

                workOrderRepository.save(it)
                return true
            }

            log.warn("삭제(비활성화)할 작업지시를 찾을 수 없습니다: {}", workOrderId)
            return false
        } catch (e: Exception) {
            log.error("작업지시 소프트 삭제 중 오류 발생", e)
            return false
        }
    }
}