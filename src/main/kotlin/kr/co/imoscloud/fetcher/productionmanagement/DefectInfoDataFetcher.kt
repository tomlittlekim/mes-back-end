package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.*
import kr.co.imoscloud.entity.productionmanagement.DefectInfo
import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.model.productionmanagement.DefectInfoFilter
import kr.co.imoscloud.service.productionmanagement.DefectInfoService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 불량 정보 GraphQL 데이터 페처
 * - 불량 정보 조회 관련 GraphQL 요청 처리
 */
@DgsComponent
class DefectInfoDataFetcher(
    private val defectInfoService: DefectInfoService
) {
    private val log = LoggerFactory.getLogger(DefectInfoDataFetcher::class.java)

    /**
     * 모든 불량 정보 조회
     * 오류가 발생해도 빈 배열을 반환해야 GraphQL non-null 타입 조건을 충족함
     */
    @DgsQuery
    fun allDefectInfos(@InputArgument filter: DefectInfoFilter?): List<DefectInfo?>? {
        try {
            return defectInfoService.getAllDefectInfos(filter)
        } catch (e: Exception) {
            log.error("불량 정보 필터 조회 중 오류 발생", e)
            return emptyList()
        }
    }

    /**
     * 생산 실적 ID로 불량 정보 조회
     */
    @DgsQuery
    fun defectInfosByProdResultId(@InputArgument prodResultId: String): List<DefectInfo> {
        try {
            return defectInfoService.getDefectInfoByProdResultId(prodResultId)
        } catch (e: Exception) {
            log.error("불량 정보 조회 중 오류 발생", e)
            return emptyList()
        }
    }

    /**
     * 생산실적에 연결된 불량 정보 목록 조회 (GraphQL 리졸버)
     */
    @DgsData(parentType = "ProductionResult", field = "defectInfos")
    fun productionResultDefectInfos(dfe: DgsDataFetchingEnvironment): List<DefectInfo> {
        try {
            val productionResult = dfe.getSource<ProductionResult>()
            val prodResultId = productionResult?.prodResultId ?: return emptyList()
            return defectInfoService.getDefectInfoByProdResultId(prodResultId)
        } catch (e: Exception) {
            log.error("생산실적 연결 불량 정보 조회 중 오류 발생", e)
            return emptyList()
        }
    }
}