package kr.co.imoscloud.fetcher.kpi

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.model.kpi.KpiChartData
import kr.co.imoscloud.model.kpi.KpiSubscriptionFilter
import kr.co.imoscloud.service.kpi.KpiChartService

/**
 * KPI 데이터 조회를 위한 GraphQL Fetcher
 */
@DgsComponent
class KpiFetcher(
    private val iotService: KpiChartService,
) {
    /**
     * 구독 중인 KPI 지표 데이터 조회 (지표별 필터 적용)
     */
    @DgsQuery
    fun getKpiChartData(@InputArgument("filters") filters: List<KpiSubscriptionFilter>): List<KpiChartData> {
        return iotService.getKpiChartData(filters)
    }
}