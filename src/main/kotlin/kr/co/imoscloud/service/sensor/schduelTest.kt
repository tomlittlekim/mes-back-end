package kr.co.imoscloud.service.sensor

import kr.co.imoscloud.entity.sensor.SensorStatus
import kr.co.imoscloud.repository.SensorStatusRep
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class schduelTest(
    val sensorStatusRep: SensorStatusRep
) {
//    @Scheduled(fixedRate = 30000)
//    fun test() {
//        val power1 = 50.0 + Math.random() * 200;
//        val power2 = 50.0 + Math.random() * 200;
//
//        val createDate = java.time.LocalDateTime.now()
//
//        sensorStatusRep.findById(1).ifPresent { sensor ->
//            // 센서 엔티티의 power 와 createDate 필드를 갱신합니다.
//            sensor.power = power1
//            sensor.createDate = createDate
//            // 업데이트한 센서를 DB에 저장합니다.
//            sensorStatusRep.save(sensor)
//        }
//
//        sensorStatusRep.findById(2).ifPresent { sensor ->
//            // 센서 엔티티의 power 와 createDate 필드를 갱신합니다.
//            sensor.power = power2
//            sensor.createDate = createDate
//            // 업데이트한 센서를 DB에 저장합니다.
//            sensorStatusRep.save(sensor)
//        }
//
//    }

}