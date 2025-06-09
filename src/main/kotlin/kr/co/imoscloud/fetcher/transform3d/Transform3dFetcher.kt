package kr.co.imoscloud.fetcher.transform3d

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.model.kpi.ChartResponseModel
import kr.co.imoscloud.service.transform3d.TransForm3dService

@DgsComponent
class Transform3dFetcher (
    val transform3dService: TransForm3dService,
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