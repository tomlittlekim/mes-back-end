package kr.co.imoscloud.service.productionmanagement.productionresult

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.model.productionmanagement.DefectInfoInput
import kr.co.imoscloud.model.productionmanagement.ProductionResultFilter
import kr.co.imoscloud.model.productionmanagement.ProductionResultInput
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
    
    // 재고 관련 서비스를 직접 주입받음
    productionInventoryService: ProductionInventoryService
) {
    // 쿼리 서비스 - 조회 관련 기능 처리
    private val queryService = ProductionResultQueryService(
        productionResultRepository,
        workOrderRepository
    )

    // 커맨드 서비스 - 생성/삭제 관련 기능 처리
    private val commandService = ProductionResultCommandService(
        productionResultRepository,
        workOrderRepository,
        defectInfoService,
        queryService,
        productionInventoryService
    )

    /**
     * 작업지시ID로 생산실적 목록 조회
     */
    fun getProductionResultsByWorkOrderId(workOrderId: String): List<ProductionResult> =
        queryService.getProductionResultsByWorkOrderId(workOrderId)

    /**
     * 다양한 필터 조건으로 생산실적 목록 조회
     */
    fun getProductionResults(filter: ProductionResultFilter): List<ProductionResult> =
        queryService.getProductionResults(filter)

    /**
     * 생산실적 저장 (생성)
     */
    fun saveProductionResult(
        createdRows: List<ProductionResultInput>? = null,
        defectInfos: List<DefectInfoInput>? = null
    ): Boolean =
        commandService.saveProductionResult(createdRows, defectInfos)

    /**
     * 생산실적 소프트 삭제 (다중)
     */
    fun softDeleteProductionResults(prodResultIds: List<String>): Boolean =
        commandService.softDeleteProductionResults(prodResultIds)
}