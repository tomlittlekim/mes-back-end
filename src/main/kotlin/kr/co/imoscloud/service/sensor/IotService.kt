package kr.co.imoscloud.service.sensor

import kr.co.imoscloud.fetcher.sensor.PowerHourFilter
import kr.co.imoscloud.repository.SensorStatusRep
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
class IotService(
    val sensorStatusRep: SensorStatusRep,
    val mongoTemplate: MongoTemplate
)
{
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val mongoDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

    fun getPowerData(): List<PowerResponseDto?> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        val result =  sensorStatusRep.getPowerData(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd
        )

        return result.map{
            PowerResponseDto(
                timeLabel = it?.createDate.toString(),
                deviceId = it?.deviceId,
                power = it?.power?:0.0
            )
        }

    }

    fun getPowerDataForWS(site: String, compCd: String): List<PowerResponseDto?> {
        val result =  sensorStatusRep.getPowerData(
            site = site,
            compCd = compCd
        )

        return result.map{
            PowerResponseDto(
                timeLabel = it?.createDate.toString(),
                deviceId = it?.deviceId,
                power = it?.power?:0.0
            )
        }

    }

    fun getPopupPowerData(filter:PowerHourFilter): List<PowerResponseDto> {
        val localDate = LocalDate.parse(filter.date, dateFormatter)

        return when(filter.range){
            "day" -> getPowerGroupedData(localDate, 0, "hour", 11, 2)
            "week" -> getPowerGroupedData(localDate, 6, "day", 0, 10)
            "month" -> getPowerGroupedData(localDate, 29, "day", 0, 10)
            else -> {
                getPowerGroupedData(localDate, 0, "hour", 11, 2)
            }
        }
    }

    fun getPowerGroupedData(
        localDate: LocalDate,
        daysRange: Long,
        groupKey: String,
        substrStart: Int,
        substrLength: Int
    ): List<PowerResponseDto> {
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
            PowerResponseDto(
                timeLabel = id.getString(groupKey),
                deviceId = id.getString("deviceid"),
                power = it.getDouble("avgPower") ?: 0.0,
            )
        }
    }

}

data class PowerResponseDto(
    val timeLabel: String,
    val deviceId: String?=null,
    val power: Double?=null,
)