package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.model.productionmanagement.WorkOrderFilter
import kr.co.imoscloud.model.productionmanagement.WorkOrderInput
import kr.co.imoscloud.model.productionmanagement.WorkOrderUpdate
import kr.co.imoscloud.model.productionmanagement.WorkOrderDeleteResult
import kr.co.imoscloud.model.productionmanagement.WorkOrderOperationResult
import kr.co.imoscloud.service.productionmanagement.WorkOrderService
import org.slf4j.LoggerFactory

@DgsComponent
class WorkOrderDataFetcher(
    private val workOrderService: WorkOrderService
) {
    private val log = LoggerFactory.getLogger(WorkOrderDataFetcher::class.java)

    // 특정 생산계획에 속한 작업지시 목록 조회
    @DgsQuery
    fun workOrdersByProdPlanId(@InputArgument("prodPlanId") prodPlanId: String): List<WorkOrder> {
        try {
            return workOrderService.getWorkOrdersByProdPlanId(prodPlanId)
        } catch (e: Exception) {
            log.error("작업지시 목록 조회 중 오류 발생", e)
            return emptyList()
        }
    }

    // 조건에 맞는 작업지시 목록 조회
    @DgsQuery
    fun workOrders(@InputArgument("filter") filter: WorkOrderFilter): List<WorkOrder> {
        try {
            // flagActive가 설정되지 않은 경우 true로 설정하여 활성화된 데이터만 조회
            val activeFilter = filter.copy(flagActive = filter.flagActive ?: true)
            return workOrderService.getWorkOrders(activeFilter)
        } catch (e: Exception) {
            log.error("작업지시 목록 조회 중 오류 발생", e)
            return emptyList()
        }
    }

    // 작업지시 저장 (생성/수정)
    @DgsData(parentType = "Mutation", field = "saveWorkOrder")
    fun saveWorkOrder(
        @InputArgument("createdRows") createdRows: List<WorkOrderInput>? = null,
        @InputArgument("updatedRows") updatedRows: List<WorkOrderUpdate>? = null
    ): Boolean {
        try {
            return workOrderService.saveWorkOrder(createdRows, updatedRows)
        } catch (e: Exception) {
            log.error("작업지시 저장 중 오류 발생", e)
            return false
        }
    }

    // 작업지시 다중 삭제 (소프트 삭제)
    @DgsData(parentType = "Mutation", field = "deleteWorkOrders")
    fun deleteWorkOrders(
        @InputArgument("workOrderIds") workOrderIds: List<String>
    ): WorkOrderDeleteResult {
        try {
            return workOrderService.softDeleteWorkOrders(workOrderIds)
        } catch (e: SecurityException) {
            log.error("인증 오류: {}", e.message)
            return WorkOrderDeleteResult(
                success = false,
                totalRequested = workOrderIds.size,
                deletedCount = 0,
                skippedCount = 0,
                skippedWorkOrders = emptyList(),
                message = "인증 오류가 발생했습니다: ${e.message}"
            )
        } catch (e: Exception) {
            log.error("작업지시 다중 삭제 중 오류 발생", e)
            return WorkOrderDeleteResult(
                success = false,
                totalRequested = workOrderIds.size,
                deletedCount = 0,
                skippedCount = 0,
                skippedWorkOrders = emptyList(),
                message = "삭제 중 오류가 발생했습니다: ${e.message}"
            )
        }
    }

    // 작업지시 다중 시작
    @DgsData(parentType = "Mutation", field = "startWorkOrders")
    fun startWorkOrders(
        @InputArgument("workOrderIds") workOrderIds: List<String>
    ): WorkOrderOperationResult {
        try {
            return workOrderService.startWorkOrders(workOrderIds)
        } catch (e: SecurityException) {
            log.error("인증 오류: {}", e.message)
            return WorkOrderOperationResult(
                success = false,
                totalRequested = workOrderIds.size,
                processedCount = 0,
                skippedCount = 0,
                skippedWorkOrders = emptyList(),
                message = "인증 오류가 발생했습니다: ${e.message}"
            )
        } catch (e: Exception) {
            log.error("작업지시 다중 시작 중 오류 발생", e)
            return WorkOrderOperationResult(
                success = false,
                totalRequested = workOrderIds.size,
                processedCount = 0,
                skippedCount = 0,
                skippedWorkOrders = emptyList(),
                message = "시작 중 오류가 발생했습니다: ${e.message}"
            )
        }
    }

    // 작업지시 다중 완료
    @DgsData(parentType = "Mutation", field = "completeWorkOrders")
    fun completeWorkOrders(
        @InputArgument("workOrderIds") workOrderIds: List<String>
    ): WorkOrderOperationResult {
        try {
            return workOrderService.completeWorkOrders(workOrderIds)
        } catch (e: SecurityException) {
            log.error("인증 오류: {}", e.message)
            return WorkOrderOperationResult(
                success = false,
                totalRequested = workOrderIds.size,
                processedCount = 0,
                skippedCount = 0,
                skippedWorkOrders = emptyList(),
                message = "인증 오류가 발생했습니다: ${e.message}"
            )
        } catch (e: Exception) {
            log.error("작업지시 다중 완료 중 오류 발생", e)
            return WorkOrderOperationResult(
                success = false,
                totalRequested = workOrderIds.size,
                processedCount = 0,
                skippedCount = 0,
                skippedWorkOrders = emptyList(),
                message = "완료 중 오류가 발생했습니다: ${e.message}"
            )
        }
    }
}