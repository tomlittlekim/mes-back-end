package kr.co.imoscloud.entity.sensor

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import java.time.LocalDateTime

@Entity
@Table(
    name = "SENSOR_STATUS",
    uniqueConstraints = [
        UniqueConstraint(
            name = "SENSOR_STATUS_pk",
            columnNames = ["SITE", "COMP_CD", "DEVICE_ID"]
        )
    ]
)
class SensorStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ", nullable = false)
    var id: Int? = null

    @Column(name = "SITE", nullable = false, length = 50)
    var site: String? = null

    @Column(name = "COMP_CD", nullable = false, length = 50)
    var compCd: String? = null

    @Column(name = "DEVICE_ID", nullable = false, length = 50)
    var deviceId: String? = null

    @Column(name = "SENSOR_TYPE", length = 50)
    var sensorType: String? = null

    @Column(name = "STATUS", length = 10)
    var status: String? = null

    @Column(name = "POWER")
    var power: Double? = null

    @Column(name = "CURRENTA")
    var currenta: Double? = null

    @Column(name = "ENERGY", nullable = false)
    var energy: Double? = null

    @Column(name = "VOLTAGE")
    var voltage: Double? = null

    @Column(name = "TEMPERATURE")
    var temperature: Double? = null

    @ColumnDefault("1")
    @Column(name = "HUMIDITY")
    var humidity: Double? = null

    @Column(name = "BATTERY")
    var battery: Double? = null

    @Column(name = "CONTRACT", length = 10)
    var contract: String? = null

    @Column(name = "VIBRATION", length = 10)
    var vibration: String? = null

    @Column(name = "MOTIONSTATE", length = 10)
    var motionstate: String? = null

    @Column(name = "CREATE_USER", length = 100)
    var createUser: String? = null

    @Column(name = "CREATE_DATE")
    var createDate: LocalDateTime? = null

    @Column(name = "UPDATE_USER", length = 100)
    var updateUser: String? = null

    @Column(name = "UPDATE_DATE")
    var updateDate: LocalDateTime? = null
}