package kr.co.imoscloud.fetcher.transform3d

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.fetcher.sensor.KpiFilter
import kr.co.imoscloud.service.sensor.ChartResponseModel
import kr.co.imoscloud.service.transform3d.*

@DgsComponent
class Transform3dFetcher (
    val transform3dService: TransForm3dService, //TODO: 쓰리디 서비스부터수
){
    @DgsQuery
    fun getPopupPowerDataFor3dView(@InputArgument("filter") filter: KpiFilterFor3dView): List<ChartResponseModel> {
        return transform3dService.getPopupPowerDataFor3DView(filter)
    }
}

data class KpiFilterFor3dView(
    var date:String,
    val range:String,
    val deviceNumber:Int,
)