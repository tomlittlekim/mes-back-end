package kr.co.imoscloud.fetcher.standardInfo

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.entity.standardInfo.Line
import kr.co.imoscloud.service.standardInfo.LineResponseModel
import kr.co.imoscloud.service.standardInfo.LineService

@DgsComponent
class LineFetcher (
    val lineService: LineService
){
    @DgsQuery
    fun getLines(@InputArgument("filter") filter:LineFilter): List<LineResponseModel?> {
        return lineService.getLines(filter)
    }

    @DgsQuery
    fun getLineOptions(): List<Line?> {
        return lineService.getLineOptions()
    }

    @DgsMutation
    fun saveLine(
        @InputArgument("createdRows") createdRows: List<LineInput?>,
        @InputArgument("updatedRows") updatedRows:List<LineUpdate?>
    ): Boolean{
        lineService.saveLine(createdRows, updatedRows)
        return true
    }

    @DgsMutation
    fun deleteLine(@InputArgument("lineId") lineId: String): Boolean{
        return lineService.deleteLine(lineId)
    }

}

data class LineFilter(
    val factoryId: String,
    val factoryName: String,
    val factoryCode: String,
    val lineId: String,
    val lineName: String,
//    val flagActive: String ?= null
)

data class LineInput(
    val factoryId: String,
    val lineName: String,
    val lineDesc: String ?= null,
//    val flagActive: String ?= null
)

data class LineUpdate(
    val lineId: String,
    val factoryId: String,
    val lineName: String,
    val lineDesc: String ?= null,
//    val flagActive: String ?= null
)