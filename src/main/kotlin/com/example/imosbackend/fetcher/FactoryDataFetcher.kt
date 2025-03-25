package com.example.imosbackend.fetcher

import com.example.imosbackend.entity.Factory
import com.example.imosbackend.entity.FactoryFilter
import com.example.imosbackend.service.FactoryResponseModel
import com.example.imosbackend.service.FactoryService
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