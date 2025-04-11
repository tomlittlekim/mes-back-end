package kr.co.imoscloud.service.productionmanagement.productionresult

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.model.productionmanagement.*
import kr.co.imoscloud.repository.productionmanagement.DefectInfoRepository
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.service.productionmanagement.DefectInfoService
import org.springframework.stereotype.Service

/**
 * 생산실적 통합 서비스 (하위 서비스로 위임)
 * - 기존 호환성을 위해 이전 구현의 메서드 유지
 * - 코드 책임을 ProductionResultQueryService와 ProductionResultCommandService로 분리
 */
@Service
class ProductionResultService(
    productionResultRepository: ProductionResultRepository,
    workOrderRepository: WorkOrderRepository,
    defectInfoService: DefectInfoService,
    defectInfoRepository: DefectInfoRepository
) {
    // 쿼리 서비스 - 조회 관련 기능 처리
    private val queryService = ProductionResultQueryService(
        productionResultRepository,
        workOrderRepository
    )

    // 커맨드 서비스 - 생성/수정/삭제 관련 기능 처리
    private val commandService = ProductionResultCommandService(
        productionResultRepository,
        workOrderRepository,
        defectInfoService,
        queryService
    )

    /**
     * 작업지시ID로 생산실적 목록 조회
     */
    fun getProductionResultsByWorkOrderId(workOrderId: String): List<ProductionResult> {
        return queryService.getProductionResultsByWorkOrderId(workOrderId)
    }

    /**
     * 다양한 필터 조건으로 생산실적 목록 조회
     */
    fun getProductionResults(filter: ProductionResultFilter): List<ProductionResult> {
        return queryService.getProductionResults(filter)
    }

    /**
     * 생산실적 요약 목록 조회
     */
    fun getProductionResultSummaryList(filter: ProductionResultInquiryFilter): List<ProductionResultSummaryDto> {
        return queryService.getProductionResultSummaryList(filter)
    }

    /**
     * 생산실적 저장 (생성/수정)
     */
    fun saveProductionResult(
        createdRows: List<ProductionResultInput>? = null,
        updatedRows: List<ProductionResultUpdate>? = null,
        defectInfos: List<DefectInfoInput>? = null
    ): Boolean {
        return commandService.saveProductionResult(createdRows, updatedRows, defectInfos)
    }

    /**
     * 생산실적 소프트 삭제
     */
    fun softDeleteProductionResult(prodResultId: String): Boolean {
        return commandService.softDeleteProductionResult(prodResultId)
    }

    /**
     * 작업지시별 총 생산 양품수량 조회
     */
    fun getTotalGoodQtyByWorkOrderId(workOrderId: String): Double {
        return queryService.getTotalGoodQtyByWorkOrderId(workOrderId)
    }
}