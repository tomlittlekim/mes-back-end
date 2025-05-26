package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.model.productionmanagement.WorkOrderFilter
import kr.co.imoscloud.model.productionmanagement.WorkOrderInput
import kr.co.imoscloud.model.productionmanagement.WorkOrderUpdate
import kr.co.imoscloud.model.productionmanagement.WorkOrderDeleteResult
import kr.co.imoscloud.model.productionmanagement.WorkOrderOperationResult
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class WorkOrderService(
    val workOrderRepository: WorkOrderRepository,
    val productionResultRepository: ProductionResultRepository
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

        return workOrderRepository.getWorkOrderList(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            workOrderId = activeFilter.workOrderId,
            prodPlanId = activeFilter.prodPlanId,
            productId = activeFilter.productId,
            shiftType = activeFilter.shiftType,
            state = activeFilter.state,
            flagActive = activeFilter.flagActive,
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
     * 작업지시 다중 시작 메서드
     *
     * @param workOrderIds 시작할 작업지시 ID 목록
     * @return 시작 결과 정보
     */
    fun startWorkOrders(workOrderIds: List<String>): WorkOrderOperationResult {
        try {
            val currentUser = getCurrentUserPrincipal()
            
            var successCount = 0
            var skippedCount = 0
            val skippedWorkOrders = mutableListOf<String>()

            workOrderIds.forEach { workOrderId ->
                val existingWorkOrder = workOrderRepository.findBySiteAndCompCdAndWorkOrderId(
                    currentUser.getSite(),
                    currentUser.compCd,
                    workOrderId
                )

                existingWorkOrder?.let {
                    it.start(currentUser)
                    workOrderRepository.save(it)
                    successCount++
                    log.info("작업지시 시작 완료: {}", workOrderId)
                } ?: run {
                    skippedCount++
                    skippedWorkOrders.add(workOrderId)
                    log.warn("시작할 작업지시를 찾을 수 없습니다: {}", workOrderId)
                }
            }

            log.info("작업지시 다중 시작 완료: 요청 {}, 처리 {}, 건너뜀 {}", workOrderIds.size, successCount, skippedCount)
            
            val message = when {
                successCount == workOrderIds.size -> "모든 작업지시가 성공적으로 시작되었습니다."
                successCount > 0 && skippedCount > 0 -> "${successCount}개 시작 완료, ${skippedCount}개는 찾을 수 없어 시작되지 않았습니다."
                successCount == 0 && skippedCount > 0 -> "시작할 수 있는 작업지시를 찾을 수 없습니다."
                else -> "시작할 수 있는 작업지시가 없습니다."
            }

            return WorkOrderOperationResult(
                success = successCount > 0 || (successCount + skippedCount) == workOrderIds.size,
                totalRequested = workOrderIds.size,
                processedCount = successCount,
                skippedCount = skippedCount,
                skippedWorkOrders = skippedWorkOrders,
                message = message
            )
        } catch (e: Exception) {
            log.error("작업지시 다중 시작 중 오류 발생", e)
            throw e
        }
    }

    /**
     * 작업지시 다중 완료 메서드
     *
     * @param workOrderIds 완료할 작업지시 ID 목록
     * @return 완료 결과 정보
     */
    fun completeWorkOrders(workOrderIds: List<String>): WorkOrderOperationResult {
        try {
            val currentUser = getCurrentUserPrincipal()
            
            var successCount = 0
            var skippedCount = 0
            val skippedWorkOrders = mutableListOf<String>()

            workOrderIds.forEach { workOrderId ->
                val existingWorkOrder = workOrderRepository.findBySiteAndCompCdAndWorkOrderId(
                    currentUser.getSite(),
                    currentUser.compCd,
                    workOrderId
                )

                existingWorkOrder?.let {
                    it.complete(currentUser)
                    workOrderRepository.save(it)
                    successCount++
                    log.info("작업지시 완료: {}", workOrderId)
                } ?: run {
                    skippedCount++
                    skippedWorkOrders.add(workOrderId)
                    log.warn("완료할 작업지시를 찾을 수 없습니다: {}", workOrderId)
                }
            }

            log.info("작업지시 다중 완료: 요청 {}, 처리 {}, 건너뜀 {}", workOrderIds.size, successCount, skippedCount)
            
            val message = when {
                successCount == workOrderIds.size -> "모든 작업지시가 성공적으로 완료되었습니다."
                successCount > 0 && skippedCount > 0 -> "${successCount}개 완료, ${skippedCount}개는 찾을 수 없어 완료되지 않았습니다."
                successCount == 0 && skippedCount > 0 -> "완료할 수 있는 작업지시를 찾을 수 없습니다."
                else -> "완료할 수 있는 작업지시가 없습니다."
            }

            return WorkOrderOperationResult(
                success = successCount > 0 || (successCount + skippedCount) == workOrderIds.size,
                totalRequested = workOrderIds.size,
                processedCount = successCount,
                skippedCount = skippedCount,
                skippedWorkOrders = skippedWorkOrders,
                message = message
            )
        } catch (e: Exception) {
            log.error("작업지시 다중 완료 중 오류 발생", e)
            throw e
        }
    }

    /**
     * 작업지시를 소프트 삭제하는 메서드 (flagActive = false로 설정)
     * 활성화된 생산실적이 있는 작업지시는 삭제할 수 없음
     */
    fun softDeleteWorkOrders(workOrderIds: List<String>): WorkOrderDeleteResult {
        try {
            val currentUser = getCurrentUserPrincipal()
            
            var deletedCount = 0
            var skippedCount = 0
            val skippedWorkOrders = mutableListOf<String>()

            workOrderIds.forEach { workOrderId ->
                // UK 필드를 활용하여 정확한 레코드 조회
                val existingWorkOrder = workOrderRepository.findBySiteAndCompCdAndWorkOrderId(
                    currentUser.getSite(),
                    currentUser.compCd,
                    workOrderId
                )

                existingWorkOrder?.let {
                    // 해당 작업지시에 연결된 활성 생산실적이 있는지 확인
                    val hasActiveProductionResults = productionResultRepository.existsBySiteAndCompCdAndWorkOrderIdAndFlagActive(
                        currentUser.getSite(),
                        currentUser.compCd,
                        workOrderId,
                        true
                    )

                    if (hasActiveProductionResults) {
                        // 활성화된 생산실적이 있으면 삭제하지 않음
                        skippedCount++
                        skippedWorkOrders.add(workOrderId)
                        log.warn("활성화된 생산실적이 있어 삭제할 수 없는 작업지시: {}", workOrderId)
                    } else {
                        // 활성화된 생산실적이 없으면 삭제 진행
                        it.softDelete(currentUser)
                        workOrderRepository.save(it)
                        deletedCount++
                    }
                } ?: log.warn("삭제(비활성화)할 작업지시 없음: {}", workOrderId)
            }

            if (skippedCount > 0) {
                log.warn("활성화된 생산실적으로 인해 삭제되지 않은 작업지시: {} ({}개)", skippedWorkOrders, skippedCount)
            }

            log.info("작업지시 다중 삭제 완료: 요청 {}, 처리 {}, 건너뜀 {}", workOrderIds.size, deletedCount, skippedCount)
            
            val message = when {
                deletedCount == workOrderIds.size -> "모든 작업지시가 성공적으로 삭제되었습니다."
                deletedCount > 0 && skippedCount > 0 -> "${deletedCount}개 삭제 완료, ${skippedCount}개는 활성화된 생산실적으로 인해 삭제되지 않았습니다."
                deletedCount == 0 && skippedCount > 0 -> "활성화된 생산실적으로 인해 삭제할 수 없는 작업지시입니다."
                else -> "삭제할 수 있는 작업지시가 없습니다."
            }

            return WorkOrderDeleteResult(
                success = deletedCount > 0 || (deletedCount + skippedCount) == workOrderIds.size,
                totalRequested = workOrderIds.size,
                deletedCount = deletedCount,
                skippedCount = skippedCount,
                skippedWorkOrders = skippedWorkOrders,
                message = message
            )
        } catch (e: Exception) {
            log.error("작업지시 소프트 삭제 중 오류 발생", e)
            throw e  // 오류를 상위로 전파하도록 변경
        }
    }
}