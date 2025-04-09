package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.model.common.GenericResponse
import kr.co.imoscloud.model.productionmanagement.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 통합 생산관리 서비스
 * - 생산계획, 작업지시, 생산실적, 불량정보를 통합적으로 관리하는 서비스
 * - 각 세부 서비스를 조합하여 고수준의 기능 제공
 */
@Service
class ProductionManagementService(
    private val productionPlanService: ProductionPlanService,
    private val workOrderService: WorkOrderService,
    private val productionResultService: ProductionResultService,
    private val defectInfoService: DefectInfoService
) {
    private val log = LoggerFactory.getLogger(ProductionManagementService::class.java)
    private val dateFormatter = DateTimeFormatter.ISO_DATE

    /**
     * 생산계획으로부터 작업지시 생성
     * - 생산계획을 기반으로 작업지시를 자동 생성
     */
    @Transactional
    fun createWorkOrdersFromPlan(prodPlanId: String): GenericResponse<List<String>> {
        try {
            // 1. 생산계획 조회
            val filter = ProductionPlanFilter(prodPlanId = prodPlanId)
            val plans = productionPlanService.getProductionPlans(filter)

            if (plans.isEmpty()) {
                return GenericResponse(
                    success = false,
                    message = "해당 생산계획을 찾을 수 없습니다: $prodPlanId",
                    data = null
                )
            }

            val plan = plans.first()

            // 2. 기존 작업지시 확인 (중복 방지)
            val workOrderFilter = WorkOrderFilter(prodPlanId = prodPlanId)
            val existingWorkOrders = workOrderService.getWorkOrders(workOrderFilter)

            if (existingWorkOrders.isNotEmpty()) {
                return GenericResponse(
                    success = false,
                    message = "이미 해당 생산계획에 연결된 작업지시가 있습니다.",
                    data = existingWorkOrders.mapNotNull { it.workOrderId }
                )
            }

            // 3. 작업지시 생성
            // 기본적으로 하나의 작업지시만 생성하지만,
            // 필요에 따라 여러 작업지시로 분할 가능 (예: 주/야간, 생산라인별)
            val workOrderInput = WorkOrderInput(
                prodPlanId = prodPlanId,
                productId = plan.productId,
                orderQty = plan.planQty,
                shiftType = plan.shiftType ?: "DAY",
                state = "PLANNED"
            )

            // 4. 작업지시 저장
            val result = workOrderService.saveWorkOrder(
                createdRows = listOf(workOrderInput),
                updatedRows = null
            )

            // 5. 결과 확인 및 응답 생성
            if (result) {
                // 생성된 작업지시 조회
                val newWorkOrders = workOrderService.getWorkOrders(workOrderFilter)
                return GenericResponse(
                    success = true,
                    message = "작업지시가 성공적으로 생성되었습니다.",
                    data = newWorkOrders.mapNotNull { it.workOrderId }
                )
            } else {
                return GenericResponse(
                    success = false,
                    message = "작업지시 생성 중 오류가 발생했습니다.",
                    data = null
                )
            }
        } catch (e: Exception) {
            log.error("작업지시 생성 중 오류 발생", e)
            return GenericResponse(
                success = false,
                message = "작업지시 생성 중 오류 발생: ${e.message}",
                data = null
            )
        }
    }

    /**
     * 작업지시로부터 생산실적 등록 및 불량정보 통합 처리
     * - 작업지시를 기반으로 생산실적과 불량정보를 함께 등록
     */
    @Transactional
    fun createProductionResultWithDefects(
        workOrderId: String,
        goodQty: Double,
        defectQty: Double,
        equipmentId: String,
        resultInfo: String? = null,
        defectCause: String? = null,
        defectInfos: List<DefectInfoInput>? = null
    ): GenericResponse<String> {
        try {
            // 1. 작업지시 조회
            val workOrderFilter = WorkOrderFilter(workOrderId = workOrderId)
            val workOrders = workOrderService.getWorkOrders(workOrderFilter)

            if (workOrders.isEmpty()) {
                return GenericResponse(
                    success = false,
                    message = "해당 작업지시를 찾을 수 없습니다: $workOrderId",
                    data = null
                )
            }

            // 2. 생산실적 입력 준비
            val productionResultInput = ProductionResultInput(
                workOrderId = workOrderId,
                goodQty = goodQty,
                defectQty = defectQty,
                equipmentId = equipmentId,
                resultInfo = resultInfo,
                defectCause = defectCause
            )

            // 3. 생산실적 저장
            val result = productionResultService.saveProductionResult(
                createdRows = listOf(productionResultInput),
                updatedRows = null,
                defectInfos = defectInfos
            )

            // 4. 결과 확인 및 응답 생성
            if (result) {
                // 생성된 생산실적 조회
                val productionResult = productionResultService.getLatestProductionResultByWorkOrder(workOrderId)

                return GenericResponse(
                    success = true,
                    message = "생산실적이 성공적으로 등록되었습니다.",
                    data = productionResult?.prodResultId
                )
            } else {
                return GenericResponse(
                    success = false,
                    message = "생산실적 등록 중 오류가 발생했습니다.",
                    data = null
                )
            }
        } catch (e: Exception) {
            log.error("생산실적 등록 중 오류 발생", e)
            return GenericResponse(
                success = false,
                message = "생산실적 등록 중 오류 발생: ${e.message}",
                data = null
            )
        }
    }

    /**
     * 생산실적 요약 정보 조회
     * - 기간별 생산실적 통계 및 불량 현황을 함께 조회
     */
    fun getProductionSummary(fromDate: String, toDate: String): GenericResponse<ProductionSummaryDto> {
        try {
            // 문자열을 LocalDate로 변환
            val from = LocalDate.parse(fromDate, dateFormatter)
            val to = LocalDate.parse(toDate, dateFormatter)

            // 1. 생산실적 통계 조회
            val productionStats = productionResultService.getProductionResultStatistics(from, to)

            // 2. 제품별 불량 통계 조회
            val defectStats = defectInfoService.getDefectStatsByProduct(from, to)

            // 3. 설비별 생산실적 조회
            val equipmentStats = productionResultService.getProductionResultByEquipment(from, to)

            // 4. 종합 요약 정보 생성
            val summary = ProductionSummaryDto(
                fromDate = fromDate,
                toDate = toDate,
                totalPlanQty = productionStats.totalPlanQty,
                totalGoodQty = productionStats.totalGoodQty,
                totalDefectQty = productionStats.totalDefectQty,
                achievementRate = productionStats.achievementRate,
                defectRate = productionStats.defectRate,
                dailyStats = productionStats.dailyStats,
                productStats = productionStats.productStats,
                equipmentStats = equipmentStats,
                defectsByProduct = defectStats
            )

            return GenericResponse(
                success = true,
                message = "생산실적 요약 정보 조회 성공",
                data = summary
            )
        } catch (e: Exception) {
            log.error("생산실적 요약 정보 조회 중 오류 발생", e)
            return GenericResponse(
                success = false,
                message = "생산실적 요약 정보 조회 중 오류 발생: ${e.message}",
                data = null
            )
        }
    }

    /**
     * 불량률 임계치 초과 제품 조회
     * - 설정된 불량률 임계치를 초과하는 제품 목록 조회
     */
    fun getProductsWithHighDefectRate(threshold: Double, fromDate: String, toDate: String): GenericResponse<List<HighDefectProductDto>> {
        try {
            // 문자열을 LocalDate로 변환
            val from = LocalDate.parse(fromDate, dateFormatter)
            val to = LocalDate.parse(toDate, dateFormatter)

            // 1. 제품별 불량 통계 조회
            val defectStats = defectInfoService.getDefectStatsByProduct(from, to)

            // 2. 불량률 임계치를 초과하는 제품 필터링
            val highDefectProducts = defectStats
                .filter { stat ->
                    val totalQty = stat.totalDefectQty
                    val defectRate = if (totalQty > 0) {
                        (stat.totalDefectQty / (stat.totalDefectQty + stat.defectCount)) * 100
                    } else 0.0

                    defectRate > threshold
                }
                .map { stat ->
                    // 주요 불량 원인 추출
                    val topCauses = stat.defectCauses
                        .sortedByDescending { it.qty }
                        .take(3)
                        .map { "${it.cause} (${it.percentage.toInt()}%)" }

                    HighDefectProductDto(
                        productId = stat.productId,
                        productName = stat.productName,
                        totalDefectQty = stat.totalDefectQty,
                        defectRate = stat.defectCauses.sumOf { it.percentage }.toString(),
                        mainCauses = topCauses
                    )
                }

            return GenericResponse(
                success = true,
                message = "불량률 임계치(${threshold}%) 초과 제품 조회 성공",
                data = highDefectProducts
            )
        } catch (e: Exception) {
            log.error("불량률 임계치 초과 제품 조회 중 오류 발생", e)
            return GenericResponse(
                success = false,
                message = "불량률 임계치 초과 제품 조회 중 오류 발생: ${e.message}",
                data = null
            )
        }
    }
}

/**
 * 생산 요약 DTO
 * - 기간 내 생산실적 종합 정보
 */
data class ProductionSummaryDto(
    val fromDate: String,
    val toDate: String,
    val totalPlanQty: Double,
    val totalGoodQty: Double,
    val totalDefectQty: Double,
    val achievementRate: String,
    val defectRate: String,
    val dailyStats: List<ProductionDailyStat>,
    val productStats: List<ProductionProductStat>,
    val equipmentStats: List<ProductionEquipmentStat>,
    val defectsByProduct: List<DefectStatsByProductDto>
)

/**
 * 높은 불량률 제품 DTO
 * - 불량률이 임계치를 초과하는 제품 정보
 */
data class HighDefectProductDto(
    val productId: String,
    val productName: String,
    val totalDefectQty: Double,
    val defectRate: String,
    val mainCauses: List<String>
)