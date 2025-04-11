package kr.co.imoscloud.repository

import kr.co.imoscloud.constants.CoreEnum
import kr.co.imoscloud.entity.sensor.SensorStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SensorStatusRep: JpaRepository<SensorStatus, Long> {

    @Query("""
        select s
        from SensorStatus s
        where s.site = :site
        and   s.compCd = :compCd
        and   s.sensorType = :sensorType
    """)
    fun getPowerData(
        site:String,
        compCd:String,
        sensorType:String = CoreEnum.SensorType.POWER.key
    ): List<SensorStatus?>
}