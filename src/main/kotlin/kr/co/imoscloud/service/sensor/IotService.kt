package kr.co.imoscloud.service.sensor

import kr.co.imoscloud.fetcher.sensor.IotFetcher.KpiChartRequest
import kr.co.imoscloud.fetcher.sensor.KpiFilter
import kr.co.imoscloud.model.kpisetting.KpiIndicatorWithCategoryModel
import kr.co.imoscloud.model.kpisetting.KpiSubscriptionModel
import kr.co.imoscloud.repository.system.KpiIndicatorRepository
import kr.co.imoscloud.repository.system.KpiSubscriptionRepository
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service

@Service
class IotService(
    val equipmentPowerService: EquipmentPowerService,
    val equipmentOperationService: EquipmentOperationService,
    val kpiProductionService: KpiProductionService,
    val kpiSubscriptionRepository: KpiSubscriptionRepository,
    val kpiIndicatorRepository: KpiIndicatorRepository
)
{
    /**
     * 지표별 필터를 적용하여 KPI 차트 데이터를 조회
     */
    fun getKpiChartDataWithFilters(request: KpiChartRequest): List<KpiChartData> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()
        val site = userPrincipal.getSite()
        val compCd = userPrincipal.compCd
        
        // 기본 필터 확인
        if (request.defaultFilter == null && (request.indicatorFilters == null || request.indicatorFilters.isEmpty())) {
            return emptyList() // 필터가 없으면 빈 목록 반환
        }
        
        // 구독 중인 KPI 지표 조회
        val activeSubscriptions = kpiSubscriptionRepository.findActiveSubscriptionsBySiteAndCompCd(site, compCd)
        if (activeSubscriptions.isEmpty()) return emptyList()
        
        // KPI 인디케이터 정보 조회 (카테고리 정보 포함)
        val kpiIndicators = kpiIndicatorRepository.findAllIndicatorsWithCategory()
        val indicatorMap = kpiIndicators.associateBy { it.kpiIndicatorCd }
        
        // 지표별 필터 맵 생성
        val indicatorFilterMap = request.indicatorFilters?.associateBy { it.kpiIndicatorCd } ?: emptyMap()
        
        // KPI 지표 코드별 데이터 수집 함수 맵핑
        val dataCollectors = mapOf(
            "kpi_001" to { f: KpiFilter -> equipmentOperationService.getEquipmentOperationData(f) },  // 설비 가동률
            "kpi_002" to { f: KpiFilter -> kpiProductionService.getProductionDefectRate(f) },    // 불량률
//            "kpi_003" to { f: KpiFilter -> getEquipmentOperationData(f) },  // 생산률 (가동률 데이터로 대체)
            "kpi_008" to { f: KpiFilter -> equipmentPowerService.getPopupPowerData(f) }           // 에너지 사용 효율
        )
        
        return activeSubscriptions.mapNotNull { subscription ->
            val kpiIndicatorCd = subscription.kpiIndicatorCd
            val indicator = indicatorMap[kpiIndicatorCd] ?: return@mapNotNull null

            val dataCollector = dataCollectors[kpiIndicatorCd] ?: return@mapNotNull null

            val filter = if (indicatorFilterMap.containsKey(kpiIndicatorCd)) {
                val customFilter = indicatorFilterMap[kpiIndicatorCd]!!
                KpiFilter(customFilter.date, customFilter.range)
            } else {
                request.defaultFilter ?: return@mapNotNull null
            }

            val chartData = dataCollector(filter)
            
            if (chartData.isNotEmpty()) {
                convertToKpiChartData(chartData, indicator, subscription)
            } else null
        }
    }
    
    /**
     * ChartResponseModel 리스트를 KpiChartData로 변환
     */
    private fun convertToKpiChartData(
        data: List<ChartResponseModel>, 
        indicator: KpiIndicatorWithCategoryModel,
        subscription: KpiSubscriptionModel
    ): KpiChartData {
        val groupedData = data.groupBy { it.timeLabel }
            .map { (timeLabel, models) ->
                val dataPoint = mutableMapOf<String, Any>("name" to timeLabel)

                models.forEach { model ->
                    dataPoint[model.label] = model.value
                }
                
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

    /**
     * 구독 중인 KPI 지표 데이터를 조회
     * 각 KPI 지표에 대한 차트 데이터를 반환
     */
    fun getSubscribedKpiData(date: String, range: String): List<KpiChartData> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()
        val site = userPrincipal.getSite()
        val compCd = userPrincipal.compCd
        
        // 구독 중인 KPI 지표 조회
        val activeSubscriptions = kpiSubscriptionRepository.findActiveSubscriptionsBySiteAndCompCd(site, compCd)
        if (activeSubscriptions.isEmpty()) return emptyList()
        
        // KPI 인디케이터 정보 조회 (카테고리 정보 포함)
        val kpiIndicators = kpiIndicatorRepository.findAllIndicatorsWithCategory()
        val indicatorMap = kpiIndicators.associateBy { it.kpiIndicatorCd }

        // 필터 생성
        val filter = KpiFilter(date, range)

        // KPI 지표 코드별 데이터 수집 함수 맵핑 (실제 DB의 kpi_xxx 코드 기반)
        val dataCollectors = mapOf(
            "kpi_001" to { f: KpiFilter -> equipmentOperationService.getEquipmentOperationData(f) },  // 설비 가동률
            "kpi_002" to { f: KpiFilter -> kpiProductionService.getProductionDefectRate(f) },    // 불량률
            "kpi_003" to { f: KpiFilter -> equipmentOperationService.getEquipmentOperationData(f) },  // 생산률 (가동률 데이터로 대체)
            "kpi_008" to { f: KpiFilter -> equipmentPowerService.getPopupPowerData(f) }           // 에너지 사용 효율
            // 추가 지표는 여기에 매핑 추가
        )

        return activeSubscriptions.mapNotNull { subscription ->
            val kpiIndicatorCd = subscription.kpiIndicatorCd
            val indicator = indicatorMap[kpiIndicatorCd] ?: return@mapNotNull null
            
            // 지표 코드에 맞는 데이터 수집기 선택
            val dataCollector = dataCollectors[kpiIndicatorCd] ?: return@mapNotNull null
            val chartData = dataCollector(filter)
            
            if (chartData.isNotEmpty()) {
                convertToKpiChartData(chartData, indicator, subscription)
            } else null
        }
    }
}

/**
 * KPI 차트 데이터 응답 모델
 */
data class KpiChartData(
    val kpiIndicatorCd: String,
    val kpiTitle: String,
    val categoryCd: String,           // 카테고리 코드
    val categoryNm: String? = null,   // 카테고리 이름
    val chartType: String,            // 차트 타입 (line, bar 등)
    val unit: String? = null,         // 단위
    val targetValue: Double? = null,  // 목표값
    val chartData: List<Map<String, Any>> // 간소화된 차트 데이터
)

data class ChartResponseModel(
    val timeLabel: String,
    val label: String,
    val value: Double,
)

data class Params(
    val daysRange: Long,
    val groupKey: String,
    val substrStart: Int,
    val substrLength: Int
)

data class ProductionRateModel(
    val site: String,
    val compCd: String,
    val planSum: Double,
    val workOrderSum: Double,
    val notWorkOrderSum: Double,
    val productionRate: Double,
    val aggregationTime: String,
)