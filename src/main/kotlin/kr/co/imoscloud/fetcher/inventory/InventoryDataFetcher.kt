package kr.co.imoscloud.fetcher.inventory

import kr.co.imoscloud.service.InventoryInManagementResponseModel
import kr.co.imoscloud.service.InventoryService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.service.InventoryInResponseModel

@DgsComponent
class InventoryDataFetcher(
    val inventoryService: InventoryService,
) {
    @DgsQuery
    fun getInventoryInManagementList(@InputArgument("filter") filter: InventoryInManagementFilter): List<InventoryInManagementResponseModel?> {
        return inventoryService.getInventoryInManagementListWithFactoryAndWarehouse(filter)
    }

    @DgsQuery
    fun getInventoryInList(@InputArgument("filter") filter: InventoryInFilter): List<InventoryInResponseModel?> {
        return inventoryService.getInventoryInListWithMaterial(filter)
    }

    @DgsMutation
    fun saveDetailedInventory(
        @InputArgument("createdRows") createdRows: List<DetailedInventoryInput?>,
        @InputArgument("updatedRows") updatedRows:List<DetailedInventoryUpdateInput?>
    ): Boolean { inventoryService.saveDetailedInventory(createdRows, updatedRows)
        return true
    }

    @DgsMutation
    fun saveInventory(@InputArgument("createdRows") createdRows: List<InventoryInMInput?>,
    ): Boolean { inventoryService.saveInventory(createdRows)
        return true
    }
    @DgsMutation
    fun deleteInventory(@InputArgument("inManagementId") inManagementId: InventoryDeleteInput,
    ): Boolean { inventoryService.deleteInventory(inManagementId)
        return true
    }

    @DgsMutation
    fun deleteDetailedInventory(@InputArgument("inInventoryId") inInventoryId: InventoryDetailDeleteInput,
    ): Boolean { inventoryService.deleteDetailInventory(inInventoryId)
        return true
    }
}

data class InventoryInFilter(
    val inManagementId: String? = null
)

data class DetailedInventoryInput(
    val inManagementId: String,
    val supplierName: String? = null,
    val manufactureName: String? = null,
    val inType: String? = null,
    val systemMaterialId: String? = null,
    val userMaterialId: String? = null,
    val materialName: String? = null,
    val materialCategory: String? = null,
    val materialStandard: String? = null,
    val qty: String? = null,
    val unitPrice: String? = null,
    val unitVat: String? = null,
    val totalPrice: String? = null,
    val createUser: String? = null,
    val createDate: String? = null,
    val updateUser: String? = null,
    val updateDate: String? = null
)

data class DetailedInventoryUpdateInput(
    val inManagementId: String,
    val inInventoryId: String,
    val supplierName: String? = null,
    val manufactureName: String? = null,
    val inType: String? = null,
    val systemMaterialId: String? = null,
    val userMaterialId: String? = null,
    val materialName: String? = null,
    val materialCategory: String? = null,
    val materialStandard: String? = null,
    val qty: String? = null,
    val unitPrice: String? = null,
    val unitVat: String? = null,
    val totalPrice: String? = null,
    val createUser: String? = null,
    val createDate: String? = null,
    val updateUser: String? = null,
    val updateDate: String? = null
)

data class InventoryInManagementFilter(
    var inManagementId: String? = null,
    var inType: String? = null,
    var factoryName: String? = null,
    var warehouseName: String? = null,
    var createUser: String? = null,
    var hasInvoice: String? = null,
    var startDate: String? = null,
    var endDate: String? = null,
)

data class InventoryInMInput(
    var site: String,
    var compCd: String,
    var inType: String,
    var factoryId: String? = null,
    var warehouseId: String? = null,
    var totalPrice: String? = null,
    var hasInvoice: String? = null,
)

data class InventoryDeleteInput (
    var site: String,
    var compCd: String,
    var inManagementId: String,
)

data class InventoryDetailDeleteInput (
    var site: String,
    var compCd: String,
    var inInventoryId: String,
)
