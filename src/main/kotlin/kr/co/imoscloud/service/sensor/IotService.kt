package kr.co.imoscloud.service.sensor

import kr.co.imoscloud.constants.CoreEnum
import kr.co.imoscloud.fetcher.sensor.IotFetcher.KpiChartRequest
import kr.co.imoscloud.fetcher.sensor.KpiFilter
import kr.co.imoscloud.model.kpisetting.KpiIndicatorWithCategoryModel
import kr.co.imoscloud.model.kpisetting.KpiSubscriptionModel
import kr.co.imoscloud.repository.SensorStatusRep
import kr.co.imoscloud.repository.productionmanagement.ProductionRateDayRep
import kr.co.imoscloud.repository.productionmanagement.ProductionRateHourRep
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.repository.system.KpiIndicatorRepository
import kr.co.imoscloud.repository.system.KpiSubscriptionRepository
import kr.co.imoscloud.service.system.CompanyService
import kr.co.imoscloud.util.SecurityUtils
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class IotService(
    val mongoTemplate: MongoTemplate,
    val companyService: CompanyService,
    val sensorStatusRep: SensorStatusRep,
    val productionRateDayRep: ProductionRateDayRep,
    val productionRateHourRep: ProductionRateHourRep,
    val productionResultRep: ProductionResultRepository,
    val kpiSubscriptionRepository: KpiSubscriptionRepository,
    val kpiIndicatorRepository: KpiIndicatorRepository
)
{
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val mongoDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

    fun getPowerData(): List<ChartResponseModel?> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        val result =  sensorStatusRep.getPowerData(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd
        )

        return result.map{
            ChartResponseModel(
                timeLabel = it?.createDate.toString(),
                label = it?.deviceId ?: "UNKNOWN",
                value = it?.power?:0.0
            )
        }
    }

    fun getPopupPowerData(filter:KpiFilter): List<ChartResponseModel> {
        val localDate = LocalDate.parse(filter.date, dateFormatter)
        val params = getParams(filter.range)

        return getPowerGroupedData(
            localDate,
            params.daysRange,
            params.groupKey,
            params.substrStart,
            params.substrLength
        )
    }

    fun getPowerDataForWS(site: String, compCd: String): List<ChartResponseModel?> {
        val result =  sensorStatusRep.getPowerData(
            site = site,
            compCd = compCd
        )

        return result.map{
            ChartResponseModel(
                timeLabel = it?.createDate.toString(),
                label = it?.deviceId?: throw IllegalArgumentException("라벨이 존재하지 않습니다. "),
                value = it.power?:0.0
            )
        }

    }

    /**
     * 전력 상세보기 MongoDB
     * */
    fun getPowerGroupedData(
        localDate: LocalDate,
        daysRange: Long,
        groupKey: String,
        substrStart: Int,
        substrLength: Int
    ): List<ChartResponseModel> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        val startStr = localDate.minusDays(daysRange).atStartOfDay().format(mongoDateTimeFormatter)
        val endStr = localDate.plusDays(1).atStartOfDay().minusSeconds(1).format(mongoDateTimeFormatter)

        val match = Aggregation.match(
            Criteria.where("vendorid").`is`(userPrincipal.compCd)
                .and("timestamp").gte(startStr).lte(endStr)
        )

        val addFieldOp = AddFieldsOperation
            .addField(groupKey)
            .withValue(Document("\$substr", listOf("\$timestamp", substrStart, substrLength)))
            .build()

        val group = Aggregation.group(groupKey, "deviceid")
            .avg("power").`as`("avgPower")

        val sort = Aggregation.sort(Sort.by(Sort.Direction.ASC, "_id.$groupKey", "_id.deviceid"))

        val aggregation = Aggregation.newAggregation(match, addFieldOp, group, sort)

        val results = mongoTemplate.aggregate(aggregation, "sensor_power", Document::class.java)

        return results.mappedResults.map {
            val id = it.get("_id", Document::class.java)
            ChartResponseModel(
                timeLabel = id.getString(groupKey),
                label = id.getString("deviceid"),
                value = it.getDouble("avgPower") ?: 0.0,
            )
        }
    }

    fun getEquipmentOperationData(filter:KpiFilter): List<ChartResponseModel> {
        val localDate = LocalDate.parse(filter.date, dateFormatter)
        val params = getParams(filter.range)

        return getEquipmentOpeGroupData(
            localDate,
            params.daysRange,
            params.groupKey,
            params.substrStart,
            params.substrLength
        )
    }

    /**
     * 설비 가동률 MongoDB
     * */
    fun getEquipmentOpeGroupData(
        localDate: LocalDate,
        daysRange: Long,
        groupKey: String,
        substrStart: Int,
        substrLength: Int
    ): List<ChartResponseModel> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        val startStr = localDate.minusDays(daysRange).atStartOfDay().format(mongoDateTimeFormatter)
        val endStr = localDate.plusDays(1).atStartOfDay().minusSeconds(1).format(mongoDateTimeFormatter)

        val (hourStart, hourEnd) = companyService.getWorkTime(userPrincipal)

        val addHoursFieldOp = AddFieldsOperation
            .addField("hours")
            .withValue(Document("\$substr", listOf("\$timestamp", 11, 2)))
            .build()

        val match = Aggregation.match(
            Criteria.where("vendorid").`is`(userPrincipal.compCd)
                .and("timestamp").gte(startStr).lte(endStr)
        )

        val matchWorkHour = Aggregation.match(
            Criteria().andOperator(
                Criteria.where("hours").gte(hourStart),
                Criteria.where("hours").lt(hourEnd) // "lt"로 해야 17:00 미만, 즉 08~16
            )
        )

        val addFieldOp = AddFieldsOperation
            .addField(groupKey)
            .withValue(Document("\$substr", listOf("\$timestamp", substrStart, substrLength)))
            .build()

        val group = Aggregation.group(groupKey, "deviceid")
            .sum(
                ConditionalOperators.`when`(
                    ComparisonOperators.Gte.valueOf("power").greaterThanEqualToValue(5)
                ).then(1).otherwise(0)
            ).`as`("activeCount")
            .count().`as`("totalCount")

        val project = Aggregation.project()
            .and("_id.$groupKey").`as`("timeLabel")
            .and("_id.deviceid").`as`("deviceId")
            .and(
                ArithmeticOperators.Multiply.valueOf(
                    ArithmeticOperators.Divide.valueOf("activeCount").divideBy("totalCount")
                ).multiplyBy(100)
            ).`as`("rate")

        val sort = Aggregation.sort(Sort.by(Sort.Direction.ASC, "timeLabel", "deviceId"))

        val aggregation = Aggregation.newAggregation(
            match, addHoursFieldOp, matchWorkHour, addFieldOp, group, project, sort
        )
        val results = mongoTemplate.aggregate(aggregation, "sensor_power", Document::class.java)

        return results.mappedResults.map {
            ChartResponseModel(
                timeLabel = it.getString("timeLabel"),
                label = it.getString("deviceId"),
                value = it.getDouble("rate") ?: 0.0
            )
        }
    }

    /**
     * 제품 불량률 구하기
     * */
    fun getProductionDefectRate(filter: KpiFilter): List<ChartResponseModel> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()
        val site = userPrincipal.getSite()
        val compCd = userPrincipal.compCd
        val companyName = userPrincipal.companyName ?: "UNKNOWN"
        val (startDate, endDate) = getDateRange(filter)

        return when (filter.range) {
            CoreEnum.DateRangeType.WEEK.value, CoreEnum.DateRangeType.MONTH.value -> {
                val entity = productionResultRep.findDayDefectRates(site, compCd, startDate, endDate)
                fillGroupData(
                    entity, companyName,
                    generateSequence(startDate.toLocalDate()) { it.plusDays(1) }
                        .takeWhile { !it.isAfter(endDate.toLocalDate().minusDays(1)) }
                        .asIterable()
                ) { it.format(dateFormatter) }
            }
            else -> {
                val entity = productionResultRep.findHourlyDefectRates(site, compCd, startDate, endDate)
                fillGroupData(
                    entity, companyName,
                    0..23
                ) { it.toString().padStart(2, '0') }
            }
        }
    }

    fun getProductionYieldRate(filter: KpiFilter): List<ChartResponseModel> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()
        val site = userPrincipal.getSite()
        val compCd = userPrincipal.compCd
        val companyName = userPrincipal.companyName ?: "UNKNOWN"
        val (startDate, endDate) = getDateRange(filter)

        return when (filter.range) {
            CoreEnum.DateRangeType.WEEK.value, CoreEnum.DateRangeType.MONTH.value -> {
                val entity = productionRateDayRep.findDayProductionYieldRates(site, compCd, startDate, endDate)
                fillGroupData(
                    entity, companyName,
                    generateSequence(startDate.minusDays(1).toLocalDate()) { it.plusDays(1) }
                        .takeWhile { !it.isAfter(endDate.toLocalDate().minusDays(2)) }
                        .asIterable()
                ) { it.format(dateFormatter) }
            }
            else -> {
                val entity = productionRateHourRep.findHourProductionYieldRates(site, compCd, startDate, endDate)
                fillGroupData(
                    entity, companyName,
                    0..23
                ) { it.toString().padStart(2, '0') }
            }
        }
    }

    private fun <T> fillGroupData(
        data: List<ChartResponseModel>,
        label: String,
        range: Iterable<T>,
        labelMapper: (T) -> String
    ): List<ChartResponseModel> {
        val map = data.associateBy { it.timeLabel }
        return range.map { t ->
            val timeLabel = labelMapper(t)
            map[timeLabel] ?: ChartResponseModel(timeLabel, label, 0.0)
        }
    }

    private fun getParams(range: String): Params = when(range) {
        CoreEnum.DateRangeType.DAY.value   -> Params(0L, "hour", 11, 2)
        CoreEnum.DateRangeType.WEEK.value  -> Params(6L, "day", 0, 10)
        CoreEnum.DateRangeType.MONTH.value -> Params(29L, "day", 0, 10)
        else    -> Params(0L, "hour", 11, 2)
    }

    private fun getDateRange(filter: KpiFilter): Pair<LocalDateTime,LocalDateTime> {
        val endDate = LocalDate.parse(filter.date, dateFormatter).plusDays(1).atStartOfDay()

        return when (filter.range) {
            CoreEnum.DateRangeType.DAY.value -> Pair(endDate.minusDays(1), endDate)
            CoreEnum.DateRangeType.WEEK.value  -> Pair(endDate.minusDays(7), endDate)
            CoreEnum.DateRangeType.MONTH.value -> Pair(endDate.minusDays(30), endDate)
            else -> Pair(endDate.minusDays(1), endDate)
        }
    }

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
            "kpi_001" to { f: KpiFilter -> getEquipmentOperationData(f) },  // 설비 가동률
            "kpi_002" to { f: KpiFilter -> getProductionDefectRate(f) },    // 불량률
//            "kpi_003" to { f: KpiFilter -> getEquipmentOperationData(f) },  // 생산률 (가동률 데이터로 대체)
            "kpi_008" to { f: KpiFilter -> getPopupPowerData(f) }           // 에너지 사용 효율
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
            "kpi_001" to { f: KpiFilter -> getEquipmentOperationData(f) },  // 설비 가동률
            "kpi_002" to { f: KpiFilter -> getProductionDefectRate(f) },    // 불량률
            "kpi_003" to { f: KpiFilter -> getEquipmentOperationData(f) },  // 생산률 (가동률 데이터로 대체)
            "kpi_008" to { f: KpiFilter -> getPopupPowerData(f) }           // 에너지 사용 효율
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