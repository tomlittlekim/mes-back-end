package kr.co.imoscloud.fetcher.standardInfo

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.service.standardInfo.FactoryResponseModel
import kr.co.imoscloud.service.standardInfo.FactoryService

@DgsComponent
class FactoryDataFetcher(
    val factoryService: FactoryService
) {
    @DgsQuery
    fun factories(@InputArgument("filter") filter: FactoryFilter): List<FactoryResponseModel?> {
        return factoryService.getFactories(filter)
    }

    @DgsMutation
    fun saveFactory(
        @InputArgument("createdRows") createdRows: List<FactoryInput?>,
        @InputArgument("updatedRows") updatedRows:List<FactoryUpdate?>
    ): Boolean {
        factoryService.saveFactory(createdRows,updatedRows)
        return true
    }

    @DgsMutation
    fun deleteFactory(@InputArgument("factoryIds") factoryIds: List<String>): Boolean {
        return factoryService.deleteFactory(factoryIds)
    }

    @DgsQuery
    fun getGridFactory(): List<FactoryResponseModel> {
        return factoryService.getGridFactory()
    }

}

data class FactoryFilter(
    val factoryId: String,
    val factoryName: String,
    val factoryCode: String,
//    val flagActive: String? = null,
)

data class FactoryInput(
    val factoryName: String,
    val factoryCode: String,
    val address: String? = null,
    val telNo: String? = null,
    val remark: String? = null,
//    val flagActive: String? = null,
)

data class FactoryUpdate(
    val factoryId: String,
    val factoryName: String,
    val factoryCode: String,
    val address: String? = null,
    val telNo: String? = null,
    val remark: String? = null,
//    val flagActive: String? = null,
)

