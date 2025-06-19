package kr.co.imoscloud.service.sensor

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class KpiMongoTimeSeriesService(
    private val mongoTemplate: MongoTemplate
) {

    fun saveKpiMongoData(dto: DeviceDataDto): DeviceData {
        val entity = DeviceData(
            timestamp         = Instant.now(),        // 서버 시각
            metadata          = dto.metadata,
            linkQuality       = dto.linkQuality,
            power             = dto.power,
            deviceTemperature = dto.deviceTemperature,
            energy            = dto.energy,
            state             = dto.state
        )

        val collectionName = "sensor_power_${dto.site}"

        return mongoTemplate.save(entity, collectionName)
    }


}

data class DeviceDataDto(
    val metadata: Map<String, Any>? = null,
    @JsonProperty("linkquality")
    val linkQuality: Int,
    val power: Int,
    @JsonProperty("device_temperature")
    val deviceTemperature: Int,
    val energy: Double,
    val state: String,
    val site: String
)

data class DeviceData(
    @Id
    val id: String? = null,

    // Time-Series 필드로 쓰일 timestamp
    val timestamp: Instant,

    val metadata: Map<String, Any>? = null,

    @JsonProperty("linkquality")
    val linkQuality: Int,

    val power: Int,

    @JsonProperty("device_temperature")
    val deviceTemperature: Int,

    val energy: Double,

    val state: String
)