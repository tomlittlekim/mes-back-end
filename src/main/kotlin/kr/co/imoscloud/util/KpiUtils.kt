package kr.co.imoscloud.util

import kr.co.imoscloud.constants.CoreEnum
import kr.co.imoscloud.model.kpi.ChartResponseModel
import kr.co.imoscloud.model.kpi.KpiFilter
import kr.co.imoscloud.model.kpi.Params
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object KpiUtils {

    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val mongoDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

    fun getParams(range: String): Params = when(range) {
        CoreEnum.DateRangeType.DAY.value   -> Params(0L, "hour", 11, 2)
        CoreEnum.DateRangeType.WEEK.value  -> Params(6L, "day", 0, 10)
        CoreEnum.DateRangeType.MONTH.value -> Params(29L, "day", 0, 10)
        else    -> Params(0L, "hour", 11, 2)
    }

    fun getDateRange(filter: KpiFilter): Pair<LocalDateTime, LocalDateTime> {
        val endDate = LocalDate.parse(filter.date, dateFormatter).plusDays(1).atStartOfDay()

        return when (filter.range) {
            CoreEnum.DateRangeType.DAY.value -> Pair(endDate.minusDays(1), endDate)
            CoreEnum.DateRangeType.WEEK.value  -> Pair(endDate.minusDays(7), endDate)
            CoreEnum.DateRangeType.MONTH.value -> Pair(endDate.minusDays(30), endDate)
            else -> Pair(endDate.minusDays(1), endDate)
        }
    }

    fun <T> fillGroupData(
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
}