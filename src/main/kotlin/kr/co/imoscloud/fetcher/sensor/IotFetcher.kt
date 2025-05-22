package kr.co.imoscloud.fetcher.sensor

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.service.sensor.*

@DgsComponent
class IotFetcher(
    val iotService: IotService,
//    val iotInfluxService: IotInfluxService,
) {

    /**
     * 실시간 전력 데이터
     * */
    @DgsQuery
    fun getPowerData(): List<ChartResponseModel?> {
        return iotService.getPowerData()
    }

    /**
     * 상세 전력 데이터
     * */
    @DgsQuery
    fun getPopupPowerData(@InputArgument("filter") filter:KpiFilter): List<ChartResponseModel> {
        return iotService.getPopupPowerData(filter)
    }

    /**
     * 설비 가동률
     * */
    @DgsQuery
    fun getEquipmentOperationData(@InputArgument("filter") filter:KpiFilter): List<ChartResponseModel> {
        return iotService.getEquipmentOperationData(filter)
    }

    /**
     * 제품 불량률
     * */
    @DgsQuery
    fun getProductDefect(@InputArgument("filter") filter:KpiFilter): List<ChartResponseModel> {
        return iotService.getProductionDefectRate(filter)
    }

}

data class KpiFilter(
    var date:String,
    val range:String
)