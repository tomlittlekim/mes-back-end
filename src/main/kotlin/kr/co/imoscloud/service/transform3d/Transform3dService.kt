package kr.co.imoscloud.service.transform3d

import kr.co.imoscloud.fetcher.transform3d.KpiFilterFor3dView
import kr.co.imoscloud.model.kpi.ChartResponseModel
import kr.co.imoscloud.model.kpi.Params
import kr.co.imoscloud.util.SecurityUtils
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class TransForm3dService(
    val mongoTemplate: MongoTemplate,
) {
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val mongoDateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

    fun getPopupPowerDataFor3DView(filter: KpiFilterFor3dView): List<ChartResponseModel> {
        val localDate = LocalDate.parse(filter.date, dateFormatter)
        val params = getParams(filter.range)

        return getPowerGroupedData(
            localDate,
            params.daysRange,
            params.groupKey,
            params.substrStart,
            params.substrLength,
            deviceNumber = filter.deviceNumber  // 프론트에서 넘겨준 숫자
        )
    }

    /**
     * 전력 상세보기 MongoDB
     * */
    private fun getPowerGroupedData(
        localDate: LocalDate,
        daysRange: Long,
        groupKey: String,
        substrStart: Int,
        substrLength: Int,
        deviceNumber: Int
    ): List<ChartResponseModel> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        val startStr = localDate.minusDays(daysRange).atStartOfDay().format(mongoDateTimeFormatter)
        val endStr = localDate.plusDays(1).atStartOfDay().minusSeconds(1).format(mongoDateTimeFormatter)

        val criteria = Criteria.where("vendorid").`is`(userPrincipal.compCd)
            .and("timestamp").gte(startStr).lte(endStr)

        // deviceid 조건 필터 추가
        if (deviceNumber != null) {
            val deviceId = "POWER_00${deviceNumber}"
            criteria.and("deviceid").`is`(deviceId)
        }

        val match = Aggregation.match(criteria)

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

    private fun getParams(range: String): Params = when(range) {
        "day"   -> Params(0L, "hour", 11, 2)
        "week"  -> Params(6L, "day", 0, 10)
        "month" -> Params(29L, "day", 0, 10)
        else    -> Params(0L, "hour", 11, 2)
    }
}