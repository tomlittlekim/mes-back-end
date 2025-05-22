package kr.co.imoscloud.service.sensor

import kr.co.imoscloud.fetcher.sensor.KpiFilter
import kr.co.imoscloud.repository.SensorStatusRep
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.service.sysrtem.CompanyService
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
    val productionResultRep: ProductionResultRepository,
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
                label = it?.deviceId ?: throw Exception(" deviceID가 존재하지 않습니다. "),
                value = it.power?:0.0
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
        val companyName = userPrincipal.companyName ?: throw IllegalArgumentException("회사명이 존재하지 않습니다. ")
        val (startDate, endDate) = getDateRange(filter)

        return when (filter.range) {
            "week", "month" -> {
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
        "day"   -> Params(0L, "hour", 11, 2)
        "week"  -> Params(6L, "day", 0, 10)
        "month" -> Params(29L, "day", 0, 10)
        else    -> Params(0L, "hour", 11, 2)
    }

    private fun getDateRange(filter: KpiFilter): Pair<LocalDateTime,LocalDateTime> {
        val endDate = LocalDate.parse(filter.date, dateFormatter).plusDays(1).atStartOfDay()

        return when (filter.range) {
            "day" -> Pair(endDate.minusDays(1), endDate)
            "week" -> Pair(endDate.minusDays(7), endDate)
            "month" -> Pair(endDate.minusDays(30), endDate)
            else -> Pair(endDate.minusDays(1), endDate)
        }
    }
}

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