package kr.co.imoscloud.fetcher.sensor

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.service.sensor.*

@DgsComponent
class IotFetcher(
    private val iotService: IotService
) {

    /**
     * 실시간 전력 데이터
     * */
    @DgsQuery
    fun getPowerData(): List<ChartResponseModel?> {
        return iotService.getPowerData()
    }

    /**
     * 상세 전력 데이터
     * */
    @DgsQuery
    fun getPopupPowerData(@InputArgument("filter") filter: KpiFilter): List<ChartResponseModel> {
        return iotService.getPopupPowerData(filter)
    }

    /**
     * 설비 가동률
     * */
    @DgsQuery
    fun getEquipmentOperationData(@InputArgument("filter") filter: KpiFilter): List<ChartResponseModel> {
        return iotService.getEquipmentOperationData(filter)
    }

    /**
     * 제품 불량률
     * */
    @DgsQuery
    fun getProductDefect(@InputArgument("filter") filter: KpiFilter): List<ChartResponseModel> {
        return iotService.getProductionDefectRate(filter)
    }
    
    /**
     * 구독 중인 KPI 지표 데이터
     * 현재 회사가 구독 중인 KPI 지표에 대한 차트 데이터를 반환
     * */
    @DgsQuery
    fun getSubscribedKpiData(@InputArgument("filter") filter: KpiFilter): List<KpiChartData> {
        return iotService.getSubscribedKpiData(filter.date, filter.range)
    }

    /**
     * KPI 구독 정보에 따른 차트 데이터 조회 (지표별 필터 지원)
     */
    @DgsQuery
    fun getKpiChartData(@InputArgument("request") request: KpiChartRequest): List<KpiChartData> {
        return iotService.getKpiChartDataWithFilters(request)
    }

    /**
     * 지표별 필터 입력 객체
     */
    data class KpiSubscriptionFilter(
        val kpiIndicatorCd: String,
        val date: String,
        val range: String
    )
    
    /**
     * KPI 차트 요청 입력 객체
     */
    data class KpiChartRequest(
        val defaultFilter: KpiFilter? = null,
        val indicatorFilters: List<KpiSubscriptionFilter>? = null
    )
}

data class KpiFilter(
    val date: String,
    val range: String
)