package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.*
import kr.co.imoscloud.entity.productionmanagement.DefectInfo
import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.model.productionmanagement.*
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.service.productionmanagement.DefectInfoService
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 불량 정보 GraphQL 데이터 페처
 * - 불량 정보 조회, 등록, 수정, 삭제, 통계 관련 GraphQL 요청 처리
 */
@DgsComponent
class DefectInfoDataFetcher(
    private val defectInfoService: DefectInfoService,
    private val workOrderRepository: WorkOrderRepository,
    private val productionResultRepository: ProductionResultRepository
) {
    /**
     * 불량 정보 목록 조회
     */
    @DgsQuery
    fun defectInfoList(@InputArgument("filter") filter: DefectInfoFilter?): List<DefectInfo> {
        val queryFilter = filter ?: DefectInfoFilter()
        return defectInfoService.getDefectInfoList(queryFilter)
    }

    /**
     * 생산 실적 ID로 불량 정보 조회
     */
    @DgsQuery
    fun defectInfoByProdResultId(@InputArgument("prodResultId") prodResultId: String): List<DefectInfo> {
        return defectInfoService.getDefectInfoByProdResultId(prodResultId)
    }

    /**
     * 제품별 불량 통계 조회
     */
    @DgsQuery
    fun defectStatsByProduct(
        @InputArgument("fromDate") fromDate: String,
        @InputArgument("toDate") toDate: String
    ): List<DefectStatsByProductDto> {
        // 문자열을 LocalDate로 변환
        val formatter = DateTimeFormatter.ISO_DATE
        val from = LocalDate.parse(fromDate, formatter)
        val to = LocalDate.parse(toDate, formatter)

        return defectInfoService.getDefectStatsByProduct(from, to)
    }

    /**
     * 원인별 불량 통계 조회
     */
    @DgsQuery
    fun defectStatsByCause(
        @InputArgument("fromDate") fromDate: String,
        @InputArgument("toDate") toDate: String
    ): List<DefectStatsByCauseDto> {
        // 문자열을 LocalDate로 변환
        val formatter = DateTimeFormatter.ISO_DATE
        val from = LocalDate.parse(fromDate, formatter)
        val to = LocalDate.parse(toDate, formatter)

        return defectInfoService.getDefectStatsByCause(from, to)
    }

    /**
     * 불량 정보 저장 (생성/수정)
     */
    @DgsData(parentType = "Mutation", field = "saveDefectInfo")
    fun saveDefectInfo(
        @InputArgument("createdRows") createdRows: List<DefectInfoInput>? = null,
        @InputArgument("updatedRows") updatedRows: List<DefectInfoUpdate>? = null
    ): Boolean {
        return defectInfoService.saveDefectInfo(createdRows, updatedRows)
    }

    /**
     * 불량 정보 삭제
     */
    @DgsData(parentType = "Mutation", field = "deleteDefectInfo")
    fun deleteDefectInfo(
        @InputArgument("defectId") defectId: String
    ): Boolean {
        return defectInfoService.deleteDefectInfo(defectId)
    }

    /**
     * 불량 정보에 연결된 작업지시 정보 조회 (GraphQL 리졸버)
     */
    @DgsData(parentType = "DefectInfo", field = "workOrder")
    fun workOrder(dfe: DgsDataFetchingEnvironment): WorkOrder? {
        val defectInfo = dfe.getSource<DefectInfo>()
        val workOrderId = defectInfo?.workOrderId ?: return null

        return workOrderRepository.findByWorkOrderId(workOrderId)
    }

    /**
     * 불량 정보에 연결된 생산실적 정보 조회 (GraphQL 리졸버)
     */
    @DgsData(parentType = "DefectInfo", field = "productionResult")
    fun productionResult(dfe: DgsDataFetchingEnvironment): ProductionResult? {
        val defectInfo = dfe.getSource<DefectInfo>()
        val prodResultId = defectInfo?.prodResultId ?: return null

        return productionResultRepository.findByProdResultId(prodResultId)
    }

    /**
     * 생산실적에 연결된 불량 정보 목록 조회 (GraphQL 리졸버)
     */
    @DgsData(parentType = "ProductionResult", field = "defectInfos")
    fun productionResultDefectInfos(dfe: DgsDataFetchingEnvironment): List<DefectInfo> {
        val productionResult = dfe.getSource<ProductionResult>()
        val prodResultId = productionResult?.prodResultId ?: return emptyList()

        return defectInfoService.getDefectInfoByProdResultId(prodResultId)
    }

    /**
     * 작업지시에 연결된 불량 정보 목록 조회 (GraphQL 리졸버)
     */
    @DgsData(parentType = "WorkOrder", field = "defectInfos")
    fun workOrderDefectInfos(dfe: DgsDataFetchingEnvironment): List<DefectInfo> {
        val workOrder = dfe.getSource<WorkOrder>()
        val workOrderId = workOrder?.workOrderId ?: return emptyList()

        val filter = DefectInfoFilter(workOrderId = workOrderId)
        return defectInfoService.getDefectInfoList(filter)
    }
}