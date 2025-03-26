package com.example.imosbackend.fetcher.inventory

import com.example.imosbackend.entity.Inventory.InventoryInMFilter
import com.example.imosbackend.entity.StandardInfo.FactoryFilter
import com.example.imosbackend.service.FactoryResponseModel
import com.example.imosbackend.service.FactoryService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument

@DgsComponent
class InventoryDataFetcher(
    val factoryService: FactoryService
) {
    @DgsQuery
    fun getInventoryList(@InputArgument("filter") filter: InventoryInMFilter): List<FactoryResponseModel?> {
        return factoryService.getFactories(filter)
    }

}