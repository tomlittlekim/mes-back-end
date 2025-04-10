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

        return productionResultRepository.getProductionResultList(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            workOrderId = activeFilter.workOrderId,
            prodResultId = activeFilter.prodResultId,
            equipmentId = activeFilter.equipmentId,
            planStartDateFrom = activeFilter.planStartDateFrom,
            planStartDateTo = activeFilter.planStartDateTo,
            flagActive = activeFilter.flagActive
        )
    }

    /**
     * 생산실적 요약 목록 조회
     */
    fun getProductionResultSummaryList(filter: ProductionResultInquiryFilter): List<ProductionResultSummaryDto> {
        val currentUser = getCurrentUserPrincipal()

        // 문자열 날짜를 LocalDateTime으로 변환
        val fromDateTime = filter.fromDate?.let {
            LocalDateTime.of(LocalDate.parse(it), LocalTime.MIN)
        }

        val toDateTime = filter.toDate?.let {
            LocalDateTime.of(LocalDate.parse(it), LocalTime.MAX)
        }

        val results = productionResultRepository.getProductionResultListWithDetails(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            workOrderId = filter.workOrderId,
            prodResultId = filter.prodResultId,
            productId = filter.productId,
            equipmentId = filter.equipmentId,
            fromDate = fromDateTime,
            toDate = toDateTime,
            status = filter.status,
            flagActive = filter.flagActive ?: true
        )

        // 생산실적 결과를 요약 DTO로 변환
        return results.map { result ->
            // 관련 작업지시 정보 조회
            val workOrder = result.workOrderId?.let {
                workOrderRepository.findByWorkOrderId(it)
            }

            // 제품명 가져오기
            val productName = workOrder?.productId?.let { getProductName(it) }

            ProductionResultSummaryDto(
                id = result.id,
                prodResultId = result.prodResultId,
                workOrderId = result.workOrderId,
                productId = workOrder?.productId,
                productName = productName,
                equipmentId = result.equipmentId,
                equipmentName = result.equipmentId?.let { getEquipmentName(it) },
                productionDate = result.createDate?.format(DateTimeFormatter.ISO_DATE),
                planQuantity = workOrder?.orderQty,
                actualQuantity = result.goodQty,
                defectQuantity = result.defectQty,
                progressRate = result.progressRate,
                defectRate = result.defectRate,
                worker = result.createUser,
                status = workOrder?.state,
                createDate = result.createDate?.format(DateTimeFormatter.ISO_DATE_TIME),
                updateDate = result.updateDate?.format(DateTimeFormatter.ISO_DATE_TIME)
            )
        }
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

    // 유틸리티 메소드들
    private fun getProductName(productId: String): String? {
        return "제품 $productId"
    }

    private fun getEquipmentName(equipmentId: String): String {
        return "설비 $equipmentId"
    }
}