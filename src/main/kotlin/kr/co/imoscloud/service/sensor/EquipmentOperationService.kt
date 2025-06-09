package kr.co.imoscloud.service.sensor

import kr.co.imoscloud.constants.CoreEnum
import kr.co.imoscloud.model.kpi.ChartResponseModel
import kr.co.imoscloud.model.kpi.KpiFilter
import kr.co.imoscloud.service.productionmanagement.ProductionPlanService
import kr.co.imoscloud.service.system.CompanyService
import kr.co.imoscloud.util.KpiUtils.dateFormatter
import kr.co.imoscloud.util.KpiUtils.getParams
import kr.co.imoscloud.util.KpiUtils.mongoDateTimeFormatter
import kr.co.imoscloud.util.SecurityUtils
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

@Service
class EquipmentOperationService(
    private val mongoTemplate: MongoTemplate,
    private val companyService: CompanyService,
) {

    private val log = LoggerFactory.getLogger(EquipmentOperationService::class.java)

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
        val start = System.currentTimeMillis()

        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        val startStr = localDate.minusDays(daysRange).atStartOfDay().format(mongoDateTimeFormatter)
        val endStr = localDate.plusDays(1).atStartOfDay().minusSeconds(1).format(mongoDateTimeFormatter)

        val (hourStart, hourEnd) = companyService.getWorkTime(userPrincipal)

        val match = Aggregation.match(
            Criteria.where("vendorid").`is`(userPrincipal.compCd)
                .and("timestamp").gte(startStr).lte(endStr)
        )

        val addHoursFieldOp = AddFieldsOperation
            .addField("hours")
            .withValue(Document("\$substr", listOf("\$timestamp", 11, 2)))
            .build()

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

        val end = System.currentTimeMillis()
        log.info("sensor_power 실행시간(ms): ${end - start}")

        return results.mappedResults.map {
            ChartResponseModel(
                timeLabel = it.getString("timeLabel"),
                label = it.getString("deviceId"),
                value = it.getDouble("rate") ?: 0.0
            )
        }
    }

    fun getEquipmentTimeOpeGroupData(
        localDate: LocalDate,
        daysRange: Long,
        groupKey: String,
        substrStart: Int,
        substrLength: Int
    ): List<ChartResponseModel> {
        val start = System.currentTimeMillis()
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        // 1. 날짜 필터 (String이 아니라 Date 객체 활용)
//        val zoneSeoul = ZoneId.of("Asia/Seoul")
//        val startKST = localDate.minusDays(daysRange).atStartOfDay()
//        val endKST = localDate.atTime(23,59,59)

        val startDate = Date.from(localDate.minusDays(daysRange).atStartOfDay().toInstant(ZoneOffset.UTC))
        val endDate = Date.from(localDate.atTime(23,59,59).toInstant(ZoneOffset.UTC))


        val (hourStart, hourEnd) = companyService.getWorkTime(userPrincipal)

        val match = Aggregation.match(
            Criteria.where("metadata.vendorid").`is`(userPrincipal.compCd)
                .and("timestamp").gte(startDate).lte(endDate)
        )

        val addFieldOp = when (groupKey) {
            CoreEnum.DateRangeType.HOUR.value -> AddFieldsOperation
                .addField("hour")
                .withValue(Document("\$hour", "\$timestamp"))
                .build()

            CoreEnum.DateRangeType.DAY.value -> AddFieldsOperation
                .addField("day")
                .withValue(
                    Document(
                        "\$dateToString", Document()
                            .append("format", "%Y-%m-%d")
                            .append("date", "\$timestamp")
                    )
                )
                .build()

            else -> throw IllegalArgumentException("지원하지 않는 groupKey: $groupKey")
        }

        val addHourFieldOp = AddFieldsOperation
            .addField("hour")
            .withValue(Document("\$hour", "\$timestamp"))
            .build()

        val matchWorkHour = Aggregation.match(
            Criteria().andOperator(
                Criteria.where("hour").gte(hourStart.toInt()),
                Criteria.where("hour").lt(hourEnd.toInt())
            )
        )

        val mongoGroupKey = groupKey

        val group = Aggregation.group(mongoGroupKey, "metadata.deviceid")
            .sum(
                ConditionalOperators.`when`(
                    ComparisonOperators.Gte.valueOf("power").greaterThanEqualToValue(5)
                ).then(1).otherwise(0)
            ).`as`("activeCount")
            .count().`as`("totalCount")

        val project = Aggregation.project()
            .and("_id.$mongoGroupKey").`as`("timeLabel")
            .and("_id.deviceid").`as`("deviceId")
            .and(
                ArithmeticOperators.Multiply.valueOf(
                    ArithmeticOperators.Divide.valueOf("activeCount").divideBy("totalCount")
                ).multiplyBy(100)
            ).`as`("rate")

        val sort = Aggregation.sort(Sort.by(Sort.Direction.ASC, "timeLabel", "deviceId"))

        val aggregationOps = mutableListOf<AggregationOperation>()
        aggregationOps.add(match)
        aggregationOps.add(addHourFieldOp)   // 항상 hour 필드 생성(업무시간 필터 위해)
        if (groupKey == CoreEnum.DateRangeType.DAY.value) aggregationOps.add(addFieldOp) // day 그룹이면 day 필드 추가
        aggregationOps.add(matchWorkHour)
        aggregationOps.add(group)
        aggregationOps.add(project)
        aggregationOps.add(sort)

        //TODO:: 추후 컬렉션 지역으로 나뉘면 collectionName Site 기준으로 조회 분리
        val aggregation = Aggregation.newAggregation(aggregationOps)
        val results = mongoTemplate.aggregate(aggregation, "sensor_power_seoul", Document::class.java)

        val end = System.currentTimeMillis()
        log.info("sensor_power 실행시간(ms): ${end - start}")

        return results.mappedResults.map {
            val timeLabel: String = when (groupKey) {
                CoreEnum.DateRangeType.HOUR.value -> it.getInteger("timeLabel").toString()
                CoreEnum.DateRangeType.DAY.value -> it.getString("timeLabel")
                else -> it.getString("timeLabel")
            }

            ChartResponseModel(
                timeLabel = timeLabel,
                label = it.getString("deviceId"),
                value = it.getDouble("rate") ?: 0.0
            )
        }
    }


//    fun getEquipmentTTimeOpeGroupData(
//        localDate: LocalDate,
//        daysRange: Long,
//        groupKey: String,
//        substrStart: Int,
//        substrLength: Int
//    ): List<ChartResponseModel> {
//        val start = System.currentTimeMillis()
//
//        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()
//
//        val startDate = localDate.minusDays(daysRange).atStartOfDay()
//        val endDate = localDate.plusDays(1).atStartOfDay().minusSeconds(1)
//
//        val (hourStart, hourEnd) = companyService.getWorkTime(userPrincipal)
//
//        val match = Aggregation.match(
//            Criteria.where("metadata.vendorid").`is`(userPrincipal.compCd)
//                .and("timestamp").gte(startDate).lte(endDate)
//        )
//
////        val addHoursFieldOp = AddFieldsOperation
////            .addField("hours")
////            .withValue(Document("\$hour", "\$timestamp"))
////            .build()
//
//        val addHoursFieldOp = AddFieldsOperation
//            .addField("hours")
//            .withValue(Document("\$substr", listOf("\$timestamp", 11, 2)))
//            .build()
//
//
//        val matchWorkHour = Aggregation.match(
//            Criteria().andOperator(
//                Criteria.where("hours").gte(hourStart),
//                Criteria.where("hours").lt(hourEnd) // "lt"로 해야 17:00 미만, 즉 08~16
//            )
//        )
//
////        val addFieldOp = AddFieldsOperation
////            .addField(groupKey)
////            .withValue(Document("\$dateTrunc", Document()
////                .append("date", "\$timestamp")
////                .append("unit", groupKey)
//////            .withValue(Document("\$substr", listOf("\$timestamp", substrStart, substrLength)))
////            )).build()
////
//
//        val addFieldOp = AddFieldsOperation
//            .addField(groupKey)
//            .withValue(Document("\$substr", listOf("\$timestamp", substrStart, substrLength)))
//            .build()
//
//        val group = Aggregation.group(groupKey, "metadata.deviceid")
//            .sum(
//                ConditionalOperators.`when`(
//                    ComparisonOperators.Gte.valueOf("power").greaterThanEqualToValue(5)
//                ).then(1).otherwise(0)
//            ).`as`("activeCount")
//            .count().`as`("totalCount")
//
//        val project = Aggregation.project()
//            .and("_id.$groupKey").`as`("timeLabel")
//            .and("_id.deviceid").`as`("deviceId")
//            .and(
//                ArithmeticOperators.Multiply.valueOf(
//                    ArithmeticOperators.Divide.valueOf("activeCount").divideBy("totalCount")
//                ).multiplyBy(100)
//            ).`as`("rate")
//
//        val sort = Aggregation.sort(Sort.by(Sort.Direction.ASC, "timeLabel", "deviceId"))
//
//        val aggregation = Aggregation.newAggregation(
//            match, addHoursFieldOp, matchWorkHour, addFieldOp, group, project, sort
//        )
//        val results = mongoTemplate.aggregate(aggregation, "sensor_power_seoul", Document::class.java)
//
//        val end = System.currentTimeMillis()
//        println("sensor_power 실행시간(ms): ${end - start}")
//
//        return results.mappedResults.map {
////            val date = it.getDate("timeLabel")
////            val formatted = when (groupKey) {
////                "hour" -> date?.let { d -> SimpleDateFormat("HH").format(d) } ?: ""
////                "day" -> date?.let { d -> SimpleDateFormat("yyyy/MM/dd").format(d) } ?: ""
////                else -> date?.toString() ?: ""
////            }
//
//            ChartResponseModel(
//                timeLabel = it.getString("timeLabel"),
//                label = it.getString("deviceId"),
//                value = it.getDouble("rate") ?: 0.0
//            )
//        }
//    }
}