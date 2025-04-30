package kr.co.imoscloud.fetcher.standardInfo

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.service.standardInfo.WarehouseResponse
import kr.co.imoscloud.service.standardInfo.WarehouseService

@DgsComponent
class WarehouseFetcher(
    private val warehouseService: WarehouseService
) {
    @DgsQuery
    fun getWarehouse(@InputArgument("filter") filter:WarehouseFilter): List<WarehouseResponse?> {
        return warehouseService.getWarehouse(filter)
    }

    @DgsMutation
    fun saveWarehouse(
        @InputArgument("createdRows") createdRows: List<WareHouseInput?>,
        @InputArgument("updatedRows") updatedRows:List<WarehouseUpdate?>
    ): Boolean {
        warehouseService.saveWarehouse(createdRows, updatedRows)
        return true
    }

    @DgsMutation
    fun deleteWarehouse(@InputArgument("warehouseId") warehouseId: String): Boolean {
        return warehouseService.deleteWarehouse(warehouseId)
    }

    @DgsQuery
    fun getGridWarehouse(): List<WarehouseResponse?> {
        return warehouseService.getWarehouse()
    }

}

data class WareHouseInput(
    val factoryId:String,
    val warehouseName:String,
    val warehouseType:String,
//    val flagActive:String
)
data class WarehouseUpdate(
    val warehouseId:String,
    val factoryId:String,
    val warehouseName:String,
    val warehouseType:String,
//    val flagActive:String
)

data class WarehouseFilter(
    val factoryId: String,
    val factoryName:String,
    val warehouseId: String,
    val warehouseName: String,
    val warehouseType: String?,
//    val flagActive:String ?= null
)