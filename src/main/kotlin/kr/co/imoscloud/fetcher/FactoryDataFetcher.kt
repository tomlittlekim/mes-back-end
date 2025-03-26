package kr.co.imoscloud.fetcher

import kr.co.imoscloud.entity.Factory
import kr.co.imoscloud.entity.FactoryFilter
import kr.co.imoscloud.service.FactoryResponseModel
import kr.co.imoscloud.service.FactoryService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument

@DgsComponent
class FactoryDataFetcher(
    val factoryService: FactoryService
) {
    @DgsQuery
    fun factories(@InputArgument("filter") filter: FactoryFilter): List<FactoryResponseModel?> {
        return factoryService.getFactories(filter)
    }
}