package kr.co.imoscloud.model.kpi


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

/**
 * KPI 필터 입력 모델
 */
data class KpiFilter(
    val date: String,  // 기준 날짜 (yyyy-MM-dd)
    val range: String  // 조회 범위 (day, week, month)
)
