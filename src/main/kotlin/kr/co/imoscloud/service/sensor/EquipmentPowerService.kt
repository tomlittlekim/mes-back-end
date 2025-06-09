package kr.co.imoscloud.service.sensor

import kr.co.imoscloud.exception.equipment.DeviceNotFoundException
import kr.co.imoscloud.model.kpi.ChartResponseModel
import kr.co.imoscloud.model.kpi.KpiFilter
import kr.co.imoscloud.repository.SensorStatusRep
import kr.co.imoscloud.util.KpiUtils
import kr.co.imoscloud.util.KpiUtils.dateFormatter
import kr.co.imoscloud.util.KpiUtils.mongoDateTimeFormatter
import kr.co.imoscloud.util.SecurityUtils
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EquipmentPowerService(
    val mongoTemplate: MongoTemplate,
    val sensorStatusRep: SensorStatusRep,
) {

    /**
     * 실시간 전력 데이터 조회
     */
    fun getRealPowerData(): List<ChartResponseModel?> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        val result =  sensorStatusRep.getPowerData(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd
        )

        return result.map{
            ChartResponseModel(
                timeLabel = it?.createDate.toString(),
                label = it?.deviceId ?: throw DeviceNotFoundException(),
                value = it.power?:0.0
            )
        }
    }

    /**
     * 전력 상세보기
     * */
    fun getPopupPowerData(filter: KpiFilter): List<ChartResponseModel> {
        val localDate = LocalDate.parse(filter.date, dateFormatter)
        val params = KpiUtils.getParams(filter.range)

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
                label = it?.deviceId?: throw DeviceNotFoundException(),
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
}