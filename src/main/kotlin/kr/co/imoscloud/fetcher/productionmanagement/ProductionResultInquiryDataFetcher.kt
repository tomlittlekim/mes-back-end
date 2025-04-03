package kr.co.imoscloud.fetcher.productionmanagement

import com.netflix.graphql.dgs.*
import kr.co.imoscloud.model.productionmanagement.*
import kr.co.imoscloud.service.productionmanagement.ProductionResultInquiryService
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 생산실적조회 GraphQL 데이터 페처
 * - 생산실적 조회, 통계, 분석 관련 GraphQL 요청 처리
 */
@DgsComponent
class ProductionResultInquiryDataFetcher(
    private val productionResultInquiryService: ProductionResultInquiryService
) {
    /**
     * 생산실적 목록 조회
     */
    @DgsQuery
    fun productionResultList(@InputArgument("filter") filter: ProductionResultInquiryFilter?): List<ProductionResultSummaryDto> {
        val queryFilter = filter ?: ProductionResultInquiryFilter()
        return productionResultInquiryService.getProductionResultList(queryFilter)
    }

    /**
     * 생산실적 상세 조회
     */
    @DgsQuery
    fun productionResultDetail(@InputArgument("prodResultId") prodResultId: String): ProductionResultInquiryDto? {
        return productionResultInquiryService.getProductionResultDetail(prodResultId)
    }

    /**
     * 생산실적 통계 조회
     */
    @DgsQuery
    fun productionResultStatistics(
        @InputArgument("fromDate") fromDate: String,
        @InputArgument("toDate") toDate: String
    ): ProductionStatisticsDto {
        // 문자열을 LocalDate로 변환
        val formatter = DateTimeFormatter.ISO_DATE
        val from = LocalDate.parse(fromDate, formatter)
        val to = LocalDate.parse(toDate, formatter)

        return productionResultInquiryService.getProductionResultStatistics(from, to)
    }

    /**
     * 설비별 생산실적 통계 조회
     */
    @DgsQuery
    fun productionResultByEquipment(
        @InputArgument("fromDate") fromDate: String,
        @InputArgument("toDate") toDate: String
    ): List<ProductionEquipmentStat> {
        // 문자열을 LocalDate로 변환
        val formatter = DateTimeFormatter.ISO_DATE
        val from = LocalDate.parse(fromDate, formatter)
        val to = LocalDate.parse(toDate, formatter)

        return productionResultInquiryService.getProductionResultByEquipment(from, to)
    }

    /**
     * 일별 통계 데이터 필드 리졸버
     */
    @DgsData(parentType = "ProductionStatistics", field = "dailyStats")
    fun getDailyStats(dfe: DgsDataFetchingEnvironment): List<ProductionDailyStat> {
        val statistics = dfe.getSource<ProductionStatisticsDto>()
        return statistics?.dailyStats ?: emptyList()
    }

    /**
     * 제품별 통계 데이터 필드 리졸버
     */
    @DgsData(parentType = "ProductionStatistics", field = "productStats")
    fun getProductStats(dfe: DgsDataFetchingEnvironment): List<ProductionProductStat> {
        val statistics = dfe.getSource<ProductionStatisticsDto>()
        return statistics?.productStats ?: emptyList()
    }
}