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
import kr.co.imoscloud.service.productionmanagement.productionresult.MobileProductionResultService
import org.slf4j.LoggerFactory

/**
 * 모바일 전용 생산실적 데이터 페처
 * - 모바일에서 생산시작/종료 관련 GraphQL 요청을 처리
 */
@DgsComponent
class MobileProductionResultDataFetcher(
    private val mobileProductionResultService: MobileProductionResultService,
) {
    private val log = LoggerFactory.getLogger(MobileProductionResultDataFetcher::class.java)

    /**
     * 모바일에서 진행 중인 생산실적 목록 조회
     */
    @DgsQuery
    fun productionResultsAtMobile(@InputArgument filter: ProductionResultFilter?): List<ProductionResult> =
        mobileProductionResultService.getProductionResultsAtMobile(filter)

    /**
     * 모바일 전용 생산실적 생성 (생산시작)
     */
    @DgsData(parentType = "Mutation", field = "startProductionAtMobile")
    fun startProductionAtMobile(
        @InputArgument("input") input: ProductionResultInput
    ): String {
        try {
            return mobileProductionResultService.saveProductionResultAtMobile(input)
        } catch (e: IllegalArgumentException) {
            // 비즈니스 로직 오류는 로그로 남기고 예외를 던짐
            log.warn("모바일 생산시작 중 비즈니스 로직 오류: ${e.message}")
            throw DgsEntityNotFoundException(e.message ?: "비즈니스 로직 오류가 발생했습니다.")
        } catch (e: Exception) {
            // 기타 예외는 에러 로그로 남기고 예외를 던짐
            log.error("모바일 생산시작 중 오류 발생", e)
            throw RuntimeException("모바일 생산시작 중 오류가 발생했습니다: ${e.message}", e)
        }
    }

    /**
     * 모바일 전용 생산실적 업데이트 (생산종료)
     */
    @DgsData(parentType = "Mutation", field = "updateProductionResultAtMobile")
    fun updateProductionResultAtMobile(
        @InputArgument("prodResultId") prodResultId: String,
        @InputArgument("input") input: ProductionResultInput,
        @InputArgument("defectInfos") defectInfos: List<DefectInfoInput>? = null
    ): Boolean {
        try {
            return mobileProductionResultService.updateProductionResultAtMobile(prodResultId, input, defectInfos)
        } catch (e: IllegalArgumentException) {
            // 비즈니스 로직 오류는 로그로 남기고 예외를 던짐
            log.warn("모바일 생산종료 중 비즈니스 로직 오류: ${e.message}")
            throw DgsEntityNotFoundException(e.message ?: "비즈니스 로직 오류가 발생했습니다.")
        } catch (e: Exception) {
            // 기타 예외는 에러 로그로 남기고 예외를 던짐
            log.error("모바일 생산종료 중 오류 발생", e)
            throw RuntimeException("모바일 생산종료 중 오류가 발생했습니다: ${e.message}", e)
        }
    }
} 