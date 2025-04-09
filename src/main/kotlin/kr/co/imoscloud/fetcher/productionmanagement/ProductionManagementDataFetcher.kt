package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.*
import kr.co.imoscloud.model.common.GenericResponse
import kr.co.imoscloud.model.productionmanagement.*
import kr.co.imoscloud.service.productionmanagement.ProductionManagementService
import kr.co.imoscloud.service.productionmanagement.HighDefectProductDto
import kr.co.imoscloud.service.productionmanagement.ProductionSummaryDto
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * 통합 생산관리 GraphQL 데이터 페처
 * - 생산관리 관련 고수준 기능을 제공하는 GraphQL 엔드포인트
 */
@DgsComponent
class ProductionManagementDataFetcher(
    private val productionManagementService: ProductionManagementService
) {
    private val log = LoggerFactory.getLogger(ProductionManagementDataFetcher::class.java)

    /**
     * 생산계획으로부터 작업지시 생성
     */
    @DgsData(parentType = "Mutation", field = "createWorkOrdersFromPlan")
    fun createWorkOrdersFromPlan(@InputArgument("prodPlanId") prodPlanId: String): Map<String, Any> {
        try {
            val response = productionManagementService.createWorkOrdersFromPlan(prodPlanId)
            return mapOf(
                "success" to response.success,
                "message" to (response.message ?: ""),
                "workOrderIds" to (response.data ?: emptyList<String>()),
                "timestamp" to response.timestamp.toString()
            )
        } catch (e: Exception) {
            log.error("작업지시 생성 중 오류 발생", e)
            return mapOf(
                "success" to false,
                "message" to "작업지시 생성 중 오류 발생: ${e.message}",
                "workOrderIds" to emptyList<String>(),
                "timestamp" to LocalDateTime.now().toString()
            )
        }
    }

    /**
     * 작업지시로부터 생산실적 등록 (불량정보 포함)
     */
    @DgsData(parentType = "Mutation", field = "createProductionResultWithDefects")
    fun createProductionResultWithDefects(
        @InputArgument("workOrderId") workOrderId: String,
        @InputArgument("goodQty") goodQty: Double,
        @InputArgument("defectQty") defectQty: Double,
        @InputArgument("equipmentId") equipmentId: String,
        @InputArgument("resultInfo") resultInfo: String?,
        @InputArgument("defectCause") defectCause: String?,
        @InputArgument("defectInfos") defectInfos: List<DefectInfoInput>?
    ): Map<String, Any> {
        try {
            val response = productionManagementService.createProductionResultWithDefects(
                workOrderId = workOrderId,
                goodQty = goodQty,
                defectQty = defectQty,
                equipmentId = equipmentId,
                resultInfo = resultInfo,
                defectCause = defectCause,
                defectInfos = defectInfos
            )

            return mapOf(
                "success" to response.success,
                "message" to (response.message ?: ""),
                "prodResultId" to (response.data ?: ""),
                "timestamp" to response.timestamp.toString()
            )
        } catch (e: Exception) {
            log.error("생산실적 등록 중 오류 발생", e)
            return mapOf(
                "success" to false,
                "message" to "생산실적 등록 중 오류 발생: ${e.message}",
                "prodResultId" to "",
                "timestamp" to LocalDateTime.now().toString()
            )
        }
    }

    /**
     * 생산 요약 정보 조회
     */
    @DgsQuery
    fun productionSummary(
        @InputArgument("fromDate") fromDate: String,
        @InputArgument("toDate") toDate: String
    ): ProductionSummaryResponse {
        try {
            val response = productionManagementService.getProductionSummary(fromDate, toDate)

            if (response.success && response.data != null) {
                return ProductionSummaryResponse(
                    success = true,
                    message = response.message ?: "생산 요약 정보 조회 성공",
                    data = response.data,
                    timestamp = response.timestamp.toString()
                )
            } else {
                return ProductionSummaryResponse(
                    success = false,
                    message = response.message ?: "생산 요약 정보 조회 실패",
                    data = null,
                    timestamp = LocalDateTime.now().toString()
                )
            }
        } catch (e: Exception) {
            log.error("생산 요약 정보 조회 중 오류 발생", e)
            return ProductionSummaryResponse(
                success = false,
                message = "생산 요약 정보 조회 중 오류 발생: ${e.message}",
                data = null,
                timestamp = LocalDateTime.now().toString()
            )
        }
    }

    /**
     * 불량률 임계치 초과 제품 조회
     */
    @DgsQuery
    fun productsWithHighDefectRate(
        @InputArgument("threshold") threshold: Double,
        @InputArgument("fromDate") fromDate: String,
        @InputArgument("toDate") toDate: String
    ): HighDefectProductResponse {
        try {
            val response = productionManagementService.getProductsWithHighDefectRate(threshold, fromDate, toDate)

            if (response.success && response.data != null) {
                return HighDefectProductResponse(
                    success = true,
                    message = response.message ?: "불량률 임계치 초과 제품 조회 성공",
                    data = response.data,
                    threshold = threshold,
                    timestamp = response.timestamp.toString()
                )
            } else {
                return HighDefectProductResponse(
                    success = false,
                    message = response.message ?: "불량률 임계치 초과 제품 조회 실패",
                    data = emptyList(),
                    threshold = threshold,
                    timestamp = LocalDateTime.now().toString()
                )
            }
        } catch (e: Exception) {
            log.error("불량률 임계치 초과 제품 조회 중 오류 발생", e)
            return HighDefectProductResponse(
                success = false,
                message = "불량률 임계치 초과 제품 조회 중 오류 발생: ${e.message}",
                data = emptyList(),
                threshold = threshold,
                timestamp = LocalDateTime.now().toString()
            )
        }
    }
}

/**
 * 생산 요약 정보 응답 클래스
 */
data class ProductionSummaryResponse(
    val success: Boolean,
    val message: String,
    val data: ProductionSummaryDto?,
    val timestamp: String
)

/**
 * 불량률 임계치 초과 제품 응답 클래스
 */
data class HighDefectProductResponse(
    val success: Boolean,
    val message: String,
    val data: List<HighDefectProductDto>,
    val threshold: Double,
    val timestamp: String
)