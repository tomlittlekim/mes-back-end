package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.*
import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.model.productionmanagement.*
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.service.productionmanagement.DefectInfoService
import kr.co.imoscloud.service.productionmanagement.ProductionResultService
import kr.co.imoscloud.service.productionmanagement.WorkOrderService
import kr.co.imoscloud.util.DateUtils
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@DgsComponent
class ProductionResultDataFetcher(
    private val productionResultService: ProductionResultService,
    private val workOrderService: WorkOrderService,
    private val workOrderRepository: WorkOrderRepository,
    private val defectInfoService: DefectInfoService
) {
    private val log = LoggerFactory.getLogger(ProductionResultDataFetcher::class.java)

    // 특정 작업지시에 속한 생산실적 목록 조회
    @DgsQuery
    fun productionResultsByWorkOrderId(@InputArgument("workOrderId") workOrderId: String): List<ProductionResult> {
        try {
            return productionResultService.getProductionResultsByWorkOrderId(workOrderId)
        } catch (e: Exception) {
            log.error("생산실적 목록 조회 중 오류 발생", e)
            return emptyList()
        }
    }

    // 조건에 맞는 생산실적 목록 조회
    @DgsQuery
    fun productionResults(@InputArgument("filter") filterInput: Map<String, Any>?): List<ProductionResult> {
        try {
            // Map으로 받은 입력값을 ProductionResultFilter로 변환
            val filter = ProductionResultFilter()

            filterInput?.let { input ->
                // 문자열 필드들 설정
                filter.workOrderId = input["workOrderId"] as? String
                filter.prodResultId = input["prodResultId"] as? String
                filter.equipmentId = input["equipmentId"] as? String

                // 날짜 필드 변환
                if (input.containsKey("planStartDateFrom")) {
                    val startDateFromStr = input["planStartDateFrom"] as? String
                    filter.planStartDateFrom = DateUtils.parseDate(startDateFromStr)
                }

                if (input.containsKey("planStartDateTo")) {
                    val startDateToStr = input["planStartDateTo"] as? String
                    filter.planStartDateTo = DateUtils.parseDate(startDateToStr)
                }

                // Boolean 필드 설정
                filter.flagActive = input["flagActive"] as? Boolean ?: true
            }

            // flagActive가 설정되지 않은 경우 true로 설정하여 활성화된 데이터만 조회
            return productionResultService.getProductionResults(filter)
        } catch (e: Exception) {
            log.error("생산실적 목록 조회 중 오류 발생", e)
            return emptyList()
        }
    }

    // 생산실적 목록 조회 (변환된 DTO 형식으로 반환)
    @DgsQuery
    fun productionResultList(@InputArgument("filter") filter: ProductionResultInquiryFilter?): List<ProductionResultSummaryDto> {
        try {
            return productionResultService.getProductionResultSummaryList(filter ?: ProductionResultInquiryFilter())
        } catch (e: Exception) {
            log.error("생산실적 요약 목록 조회 중 오류 발생", e)
            return emptyList()
        }
    }

    // 생산실적 상세 조회
    @DgsQuery
    fun productionResultDetail(@InputArgument("prodResultId") prodResultId: String): ProductionResultDetailDto? {
        try {
            return productionResultService.getProductionResultDetail(prodResultId)
        } catch (e: Exception) {
            log.error("생산실적 상세 조회 중 오류 발생", e)
            return null
        }
    }

    // 생산실적 통계 조회
    @DgsQuery
    fun productionResultStatistics(
        @InputArgument("fromDate") fromDate: String,
        @InputArgument("toDate") toDate: String
    ): ProductionStatisticsDto {
        try {
            // 문자열을 LocalDate로 변환
            val formatter = DateTimeFormatter.ISO_DATE
            val from = LocalDate.parse(fromDate, formatter)
            val to = LocalDate.parse(toDate, formatter)

            return productionResultService.getProductionResultStatistics(from, to)
        } catch (e: Exception) {
            log.error("생산실적 통계 조회 중 오류 발생", e)
            // 빈 통계 객체 반환
            return ProductionStatisticsDto(
                fromDate = fromDate,
                toDate = toDate,
                totalPlanQty = 0.0,
                totalGoodQty = 0.0,
                totalDefectQty = 0.0,
                achievementRate = "0.0",
                defectRate = "0.0",
                dailyStats = emptyList(),
                productStats = emptyList()
            )
        }
    }

    // 설비별 생산실적 통계 조회
    @DgsQuery
    fun productionResultByEquipment(
        @InputArgument("fromDate") fromDate: String,
        @InputArgument("toDate") toDate: String
    ): List<ProductionEquipmentStat> {
        try {
            // 문자열을 LocalDate로 변환
            val formatter = DateTimeFormatter.ISO_DATE
            val from = LocalDate.parse(fromDate, formatter)
            val to = LocalDate.parse(toDate, formatter)

            return productionResultService.getProductionResultByEquipment(from, to)
        } catch (e: Exception) {
            log.error("설비별 생산실적 통계 조회 중 오류 발생", e)
            return emptyList()
        }
    }

    // 작업지시와 생산실적 통합 조회 (UI에서 필요한 경우)
    @DgsQuery
    fun workOrdersWithProductionResults(@InputArgument("filter") filter: WorkOrderFilter): List<WorkOrder> {
        try {
            // flagActive가 설정되지 않은 경우 true로 설정하여 활성화된 데이터만 조회
            val activeFilter = filter.copy(flagActive = filter.flagActive ?: true)
            // 단순히 작업지시 목록을 반환 (GraphQL 리졸버를 통해 생산실적을 채움)
            return workOrderService.getWorkOrders(activeFilter)
        } catch (e: Exception) {
            log.error("작업지시 목록 조회 중 오류 발생", e)
            return emptyList()
        }
    }

    // 생산실적 저장 (생성/수정)
    @DgsData(parentType = "Mutation", field = "saveProductionResult")
    fun saveProductionResult(
        @InputArgument("createdRows") createdRows: List<ProductionResultInput>? = null,
        @InputArgument("updatedRows") updatedRows: List<ProductionResultUpdate>? = null,
        @InputArgument("defectInfos") defectInfos: List<DefectInfoInput>? = null
    ): Boolean {
        try {
            return productionResultService.saveProductionResult(createdRows, updatedRows, defectInfos)
        } catch (e: Exception) {
            log.error("생산실적 저장 중 오류 발생", e)
            return false
        }
    }

    // 생산실적 삭제 (소프트 삭제로 변경)
    @DgsData(parentType = "Mutation", field = "deleteProductionResult")
    fun deleteProductionResult(
        @InputArgument("prodResultId") prodResultId: String
    ): Boolean {
        try {
            return productionResultService.softDeleteProductionResult(prodResultId)
        } catch (e: Exception) {
            log.error("생산실적 삭제 중 오류 발생", e)
            return false
        }
    }

    // 생산실적에 연결된 작업지시 정보 조회 (GraphQL 리졸버)
    @DgsData(parentType = "ProductionResult", field = "workOrder")
    fun workOrder(dfe: DgsDataFetchingEnvironment): WorkOrder? {
        try {
            val productionResult = dfe.getSource<ProductionResult>()
            val workOrderId = productionResult?.workOrderId ?: return null

            // 활성화된 작업지시만 조회
            return workOrderRepository.findByWorkOrderId(workOrderId)?.let {
                if (it.flagActive == true) it else null
            }
        } catch (e: Exception) {
            log.error("작업지시 조회 중 오류 발생", e)
            return null
        }
    }

    // 생산통계의 일별 통계 데이터 필드 리졸버
    @DgsData(parentType = "ProductionStatistics", field = "dailyStats")
    fun getDailyStats(dfe: DgsDataFetchingEnvironment): List<ProductionDailyStat> {
        val statistics = dfe.getSource<ProductionStatisticsDto>()
        return statistics?.dailyStats ?: emptyList()
    }

    // 생산통계의 제품별 통계 데이터 필드 리졸버
    @DgsData(parentType = "ProductionStatistics", field = "productStats")
    fun getProductStats(dfe: DgsDataFetchingEnvironment): List<ProductionProductStat> {
        val statistics = dfe.getSource<ProductionStatisticsDto>()
        return statistics?.productStats ?: emptyList()
    }
}