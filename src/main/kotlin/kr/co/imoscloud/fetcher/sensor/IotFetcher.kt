package kr.co.imoscloud.fetcher.sensor

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.entity.sensor.SensorStatus
import kr.co.imoscloud.service.sensor.IotService
import kr.co.imoscloud.service.sensor.PowerResponseDto

@DgsComponent
class IotFetcher(
    val iotService: IotService,
) {
    @DgsQuery
    fun getPowerData(): List<PowerResponseDto?> {
        return iotService.getPowerData()
    }

    @DgsQuery
    fun getPopupPowerData(@InputArgument("filter") filter:PowerHourFilter): List<PowerResponseDto> {
        return iotService.getPopupPowerData(filter)
    }

}

data class PowerHourFilter(
    var date:String,
    val range:String
)