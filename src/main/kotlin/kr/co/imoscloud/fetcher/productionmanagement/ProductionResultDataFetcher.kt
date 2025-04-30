package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.netflix.graphql.dgs.exceptions.DgsEntityNotFoundException
import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.model.productionmanagement.DefectInfoInput
import kr.co.imoscloud.model.productionmanagement.ProductionResultFilter
import kr.co.imoscloud.model.productionmanagement.ProductionResultInput
import kr.co.imoscloud.service.productionmanagement.productionresult.ProductionResultService
import kr.co.imoscloud.util.DateUtils
import org.slf4j.LoggerFactory

@DgsComponent
class ProductionResultDataFetcher(
    private val productionResultService: ProductionResultService,
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
                filter.productId = input["productId"] as? String
                filter.equipmentId = input["equipmentId"] as? String

                // 날짜 필드 변환 - 정확한 시간까지 파싱하는 메소드 사용
                if (input.containsKey("prodStartTimeFrom")) {
                    val prodStartTimeFrom = input["prodStartTimeFrom"] as? String
                    filter.prodStartTimeFrom = DateUtils.parseDateTimeExact(prodStartTimeFrom)
                }
                if (input.containsKey("prodStartTimeTo")) {
                    val prodStartTimeTo = input["prodStartTimeTo"] as? String
                    filter.prodStartTimeTo = DateUtils.parseDateTimeExact(prodStartTimeTo)
                }
                if (input.containsKey("prodEndTimeFrom")) {
                    val prodEndTimeFrom = input["prodEndTimeFrom"] as? String
                    filter.prodEndTimeFrom = DateUtils.parseDateTimeExact(prodEndTimeFrom)
                }
                if (input.containsKey("prodEndTimeTo")) {
                    val prodEndTimeTo = input["prodEndTimeTo"] as? String
                    filter.prodEndTimeTo = DateUtils.parseDateTimeExact(prodEndTimeTo)
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

    // 생산실적 저장 (생성)
    @DgsData(parentType = "Mutation", field = "saveProductionResult")
    fun saveProductionResult(
        @InputArgument("createdRows") createdRows: List<ProductionResultInput>? = null,
        @InputArgument("defectInfos") defectInfos: List<DefectInfoInput>? = null
    ): Boolean {
        try {
            return productionResultService.saveProductionResult(createdRows, defectInfos)
        } catch (e: IllegalArgumentException) {
            // 비즈니스 로직 오류는 로그로 남기고 예외를 던짐
            log.warn("생산실적 저장 중 비즈니스 로직 오류: ${e.message}")
            throw DgsEntityNotFoundException(e.message ?: "비즈니스 로직 오류가 발생했습니다.")
        } catch (e: Exception) {
            // 기타 예외는 에러 로그로 남기고 예외를 던짐
            log.error("생산실적 저장 중 오류 발생", e)
            throw RuntimeException("생산실적 저장 중 오류가 발생했습니다: ${e.message}", e)
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
}