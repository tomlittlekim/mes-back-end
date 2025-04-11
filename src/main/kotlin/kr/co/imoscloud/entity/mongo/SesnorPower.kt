package kr.co.imoscloud.entity.mongo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "sensor_power")
data class SensorPower(
    @Id
    var id: String? = null,
    @Field("timestamp")
    val timeStamp: String,

    @Field("deviceid")
    val deviceId: String,

    @Field("vendorid")
    val vendorId: String,

    @Field("linkquality")
    val linkQuality: Int? = null,

    @Field("device_temperature")
    val deviceTemperature: Int? = null,

    val power: Int?,
    val energy: Double?,
    val state: String?,
)