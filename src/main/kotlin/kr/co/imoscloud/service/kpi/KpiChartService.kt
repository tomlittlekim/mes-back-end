package kr.co.imoscloud.service.kpi

import kr.co.imoscloud.model.kpi.ChartResponseModel
import kr.co.imoscloud.model.kpi.KpiChartData
import kr.co.imoscloud.model.kpi.KpiFilter
import kr.co.imoscloud.model.kpi.KpiSubscriptionFilter
import kr.co.imoscloud.model.kpisetting.KpiIndicatorWithCategoryModel
import kr.co.imoscloud.model.kpisetting.KpiSubscriptionModel
import kr.co.imoscloud.repository.system.KpiIndicatorRepository
import kr.co.imoscloud.repository.system.KpiSubscriptionRepository
import kr.co.imoscloud.service.sensor.EquipmentOperationService
import kr.co.imoscloud.service.sensor.EquipmentPowerService
import kr.co.imoscloud.service.sensor.KpiProductionService
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service

@Service
class KpiChartService(
    private val equipmentPowerService: EquipmentPowerService,
    private val equipmentOperationService: EquipmentOperationService,
    private val kpiProductionService: KpiProductionService,
    private val kpiSubscriptionRepository: KpiSubscriptionRepository,
    private val kpiIndicatorRepository: KpiIndicatorRepository
) {
    /**
     * KPI 차트 데이터 조회 (지표별 필터 적용)
     * 각 KPI 지표에 해당하는 필터로 데이터를 조회
     * kpiIndicatorCd가 없는 경우 사용자의 모든 구독 정보 기준으로 데이터 조회
     */
    fun getKpiChartData(filters: List<KpiSubscriptionFilter>): List<KpiChartData> {
        // 필터 유효성 검사
        if (filters.isEmpty()) {
            return emptyList()
        }

        // 적어도 하나의 날짜와 범위 정보가 필요
        val defaultFilter = filters.firstOrNull() ?: return emptyList()
        val userInfo = getCurrentUserInfo()

        // kpiIndicatorCd가 있는 필터들만 추출해서 맵으로 변환
        val indicatorFilterMap = filters
            .filter { it.kpiIndicatorCd != null }
            .associateBy { it.kpiIndicatorCd!! }

        // 특정 지표 코드가 지정된 경우, 해당 지표들만 처리
        val specificIndicators = indicatorFilterMap.keys

        // 사용자가 구독 중인 활성화된 KPI 지표 목록 조회
        val activeSubscriptions = getActiveSubscriptions(userInfo.first, userInfo.second)
            .let { subs ->
                // 특정 지표 코드가 지정된 경우, 해당 지표들만 필터링
                if (specificIndicators.isNotEmpty()) {
                    subs.filter { it.kpiIndicatorCd in specificIndicators }
                } else {
                    subs
                }
            }

        if (activeSubscriptions.isEmpty()) return emptyList()
        val indicatorMap = getIndicatorsWithCategory()

        return activeSubscriptions.mapNotNull { subscription ->
            val kpiIndicatorCd = subscription.kpiIndicatorCd
            val indicator = indicatorMap[kpiIndicatorCd] ?: return@mapNotNull null

            // 해당 지표에 대한 필터 가져오기 (없으면 기본 필터 사용)
            val filterInfo = indicatorFilterMap[kpiIndicatorCd] ?: defaultFilter
            val filter = KpiFilter(filterInfo.date, filterInfo.range)

            // 데이터 수집 및 변환
            val chartData = collectKpiData(kpiIndicatorCd, filter) ?: return@mapNotNull null
            if (chartData.isEmpty()) return@mapNotNull null

            convertToKpiChartData(chartData, indicator, subscription)
        }
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    private fun getCurrentUserInfo(): Pair<String, String> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()
        val site = userPrincipal.getSite()
        val compCd = userPrincipal.compCd
        return Pair(site, compCd)
    }

    /**
     * 활성화된 KPI 구독 정보 조회
     */
    private fun getActiveSubscriptions(site: String, compCd: String): List<KpiSubscriptionModel> {
        return kpiSubscriptionRepository.findActiveSubscriptionsBySiteAndCompCd(site, compCd)
    }

    /**
     * KPI 인디케이터 정보와 카테고리 정보 조회
     */
    private fun getIndicatorsWithCategory(): Map<String, KpiIndicatorWithCategoryModel> {
        return kpiIndicatorRepository.findAllIndicatorsWithCategory()
            .associateBy { it.kpiIndicatorCd }
    }

    /**
     * KPI 지표 코드에 따른 데이터 수집
     */
    private fun collectKpiData(kpiIndicatorCd: String, filter: KpiFilter): List<ChartResponseModel>? {
        // KPI 지표 코드별 데이터 수집 함수 매핑
        val dataCollector = when (kpiIndicatorCd) {
            "kpi_001" -> { f: KpiFilter -> kpiProductionService.getProductionYieldRate(f) }  // 설비 가동률
            "kpi_002" -> { f: KpiFilter -> kpiProductionService.getProductionDefectRate(f) }         // 불량률
            "kpi_003" -> { f: KpiFilter -> equipmentOperationService.getEquipmentOperationData(f) }  // 생산률
            "kpi_008" -> { f: KpiFilter -> equipmentPowerService.getPopupPowerData(f) }              // 에너지 사용 효율
            else -> null
        }

        return dataCollector?.invoke(filter)
    }

    /**
     * 차트 데이터 변환
     */
    private fun convertToKpiChartData(
        data: List<ChartResponseModel>,
        indicator: KpiIndicatorWithCategoryModel,
        subscription: KpiSubscriptionModel
    ): KpiChartData {
        val groupedData = data.groupBy { it.timeLabel }
            .map { (timeLabel, models) ->
                val dataPoint = mutableMapOf<String, Any>("name" to timeLabel)
                models.forEach { model -> dataPoint[model.label] = model.value }
                dataPoint
            }
            .sortedBy { it["name"] as String }

        return KpiChartData(
            kpiIndicatorCd = subscription.kpiIndicatorCd,
            kpiTitle = subscription.description ?: indicator.kpiIndicatorNm ?: "KPI 지표",
            categoryCd = indicator.categoryCd,
            categoryNm = indicator.categoryNm,
            chartType = indicator.chartType ?: "line",
            unit = indicator.unit,
            targetValue = indicator.targetValue,
            chartData = groupedData
        )
    }
}