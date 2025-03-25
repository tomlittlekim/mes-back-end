package com.example.imosbackend.fetcher.StandardInfo

import com.example.imosbackend.entity.StandardInfo.FactoryFilter
import com.example.imosbackend.entity.StandardInfo.FactoryInput
import com.example.imosbackend.service.FactoryResponseModel
import com.example.imosbackend.service.FactoryService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
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

    @DgsMutation
    fun createFactory(@InputArgument("input") factoryInput: List<FactoryInput>): Boolean {
        factoryService.createFactory(factoryInput)
        return true
    }

    @DgsMutation
    fun deleteFactory(@InputArgument("factoryId") factoryId: String): Boolean {
        return factoryService.deleteFactory(factoryId)
    }

}
