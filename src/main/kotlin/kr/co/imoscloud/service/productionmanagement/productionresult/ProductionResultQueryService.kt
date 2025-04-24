package kr.co.imoscloud.service.productionmanagement.productionresult

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.model.productionmanagement.*
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 생산실적 조회 관련 서비스
 */
@Service
class ProductionResultQueryService(
    private val productionResultRepository: ProductionResultRepository,
    private val workOrderRepository: WorkOrderRepository
) {
    /**
     * 작업지시ID로 생산실적 목록 조회
     */
    fun getProductionResultsByWorkOrderId(workOrderId: String): List<ProductionResult> {
        val currentUser = getCurrentUserPrincipal()
        return productionResultRepository.getProductionResultsByWorkOrderId(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            workOrderId = workOrderId
        )
    }

    /**
     * 다양한 필터 조건으로 생산실적 목록 조회
     */
    fun getProductionResults(filter: ProductionResultFilter): List<ProductionResult> {
        val currentUser = getCurrentUserPrincipal()
        // flagActive가 명시적으로 설정되지 않은 경우 true로 설정하여 활성화된 데이터만 조회
        val activeFilter = filter.copy(flagActive = filter.flagActive ?: true)

        return productionResultRepository.getProductionResults(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            workOrderId = activeFilter.workOrderId,
            prodResultId = activeFilter.prodResultId,
            productId = activeFilter.productId,
            equipmentId = activeFilter.equipmentId,
            warehouseId = activeFilter.warehouseId,
            prodStartTimeFrom = activeFilter.prodStartTimeFrom,
            prodStartTimeTo = activeFilter.prodStartTimeTo,
            prodEndTimeFrom = activeFilter.prodEndTimeFrom,
            prodEndTimeTo = activeFilter.prodEndTimeTo,
            flagActive = activeFilter.flagActive
        )
    }

    /**
     * 작업지시별 총 생산 양품수량 조회
     * - 특정 작업지시ID에 대한 모든 생산실적의 양품수량 합계를 반환
     */
    fun getTotalGoodQtyByWorkOrderId(workOrderId: String): Double {
        val currentUser = getCurrentUserPrincipal()
        val results = productionResultRepository.getProductionResultsByWorkOrderId(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            workOrderId = workOrderId
        )

        // 현재 편집 중인 생산실적을 제외한 모든 생산실적의 양품수량 합계 계산
        return results.sumOf { it.goodQty ?: 0.0 }
    }

    fun getProductionResultsAtMobile(filter: ProductionResultFilter?): List<ProductionResult> {
        val currentUser = getCurrentUserPrincipal()
        return productionResultRepository.getProductionResultsAtMobile(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            filter = filter
        )
    }

}