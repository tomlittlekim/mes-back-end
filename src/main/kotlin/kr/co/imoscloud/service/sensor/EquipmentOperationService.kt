package kr.co.imoscloud.service.sensor

import kr.co.imoscloud.model.kpi.ChartResponseModel
import kr.co.imoscloud.model.kpi.KpiFilter
import kr.co.imoscloud.service.system.CompanyService
import kr.co.imoscloud.util.KpiUtils.dateFormatter
import kr.co.imoscloud.util.KpiUtils.getParams
import kr.co.imoscloud.util.KpiUtils.mongoDateTimeFormatter
import kr.co.imoscloud.util.SecurityUtils
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EquipmentOperationService(
    private val mongoTemplate: MongoTemplate,
    private val companyService: CompanyService,
) {
    /**
     * 설비 가동률 데이터 조회
     */
    fun getEquipmentOperationData(filter: KpiFilter): List<ChartResponseModel> {
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
}