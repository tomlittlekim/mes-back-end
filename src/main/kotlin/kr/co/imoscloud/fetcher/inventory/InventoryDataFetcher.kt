package kr.co.imoscloud.fetcher.inventory

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.entity.inventory.InventoryHistory
import kr.co.imoscloud.service.inventory.*

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
    fun saveInventoryInManagement(@InputArgument("createdRows") createdRows: List<InventoryInManagementSaveInput?>,
    ): Boolean { inventoryService.saveInventoryInManagement(createdRows)
        return true
    }
    @DgsMutation
    fun saveInventoryIn(
        @InputArgument("createdRows") createdRows: List<InventoryInSaveInput?>,
        @InputArgument("updatedRows") updatedRows:List<InventoryInUpdateInput?>
    ): Boolean { inventoryService.saveInventoryIn(createdRows, updatedRows)
        return true
    }
    @DgsMutation
    fun deleteInventoryInManagement(@InputArgument("inManagementId") inManagementId: InventoryInManagementDeleteInput,
    ): Boolean { inventoryService.deleteInventoryInManagement(inManagementId)
        return true
    }
    @DgsMutation
    fun deleteInventoryIn(@InputArgument("inInventoryId") inInventoryId: InventoryInDeleteInput,
    ): Boolean { inventoryService.deleteInventoryIn(inInventoryId)
        return true
    }

    //출고 관리
    @DgsQuery
    fun getInventoryOutManagementList(@InputArgument("filter") filter: InventoryOutManagementFilter): List<InventoryOutManagementResponseModel?> {
        return inventoryService.getInventoryOutManagementListWithFactoryAndWarehouse(filter)
    }
    @DgsQuery
    fun getInventoryOutList(@InputArgument("filter") filter: InventoryOutFilter): List<InventoryOutResponseModel?> {
        return inventoryService.getInventoryOutListWithMaterial(filter)
    }
    @DgsMutation
    fun saveInventoryOutManagement(@InputArgument("createdRows") createdRows: List<InventoryOutManagementSaveInput?>,
    ): Boolean { inventoryService.saveInventoryOutManagement(createdRows)
        return true
    }
    @DgsMutation
    fun saveInventoryOut(
        @InputArgument("createdRows") createdRows: List<InventoryOutSaveInput?>,
        @InputArgument("updatedRows") updatedRows:List<InventoryOutUpdateInput?>?
    ): Boolean { inventoryService.saveInventoryOut(createdRows, updatedRows ?: emptyList())
        return true
    }
    @DgsMutation
    fun deleteInventoryOutManagement(@InputArgument("outManagementId") outManagementId: InventoryOutManagementDeleteInput,
    ): Boolean { inventoryService.deleteInventoryOutManagement(outManagementId)
        return true
    }
    @DgsMutation
    fun deleteInventoryOut(@InputArgument("outInventoryId") outInventoryId: InventoryOutDeleteInput,
    ): Boolean { inventoryService.deleteInventoryOut(outInventoryId)
        return true
    }
    //재고 현황
    @DgsQuery
    fun getInventoryStatusList(@InputArgument("filter") filter: InventoryStatusFilter?): List<InventoryStatusResponseModel?> {
        return inventoryService.getInventoryStatusWithJoinInfo(filter ?: InventoryStatusFilter())
    }
    //재고 이력
    @DgsQuery
    fun getInventoryHistoryList(@InputArgument("filter") filter: InventoryHistoryFilter): List<InventoryHistory?> {
        return inventoryService.getInventoryHistoryList(filter)
    }
}

// 입고관리 DTO
data class InventoryInFilter(
    val inManagementId: String? = null,
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
data class InventoryStatusFilter(
    val warehouseName: String? = null,
    val supplierName: String? = null,
    val manufactureName: String? = null,
    val materialName: String? = null,
)
data class InventoryHistoryFilter(
    var warehouseName : String? = null,
    var inOutType : String? = null,
    var supplierName : String? = null,
    var manufacturerName : String? = null,
    var materialName : String? = null,
    var startDate : String? = null,
    var endDate : String? = null,
)
data class InventoryInManagementSaveInput(
    var inType: String,
    var factoryId: String? = null,
    var warehouseId: String? = null,
    var totalPrice: String? = null,
    var hasInvoice: String? = null,
)
data class InventoryInSaveInput(
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
data class InventoryInUpdateInput(
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
data class InventoryInManagementDeleteInput (
    var inManagementId: String,
)
data class InventoryInDeleteInput (
    var inInventoryId: String,
)

// 출고관리 DTO
data class InventoryOutFilter(
    val outManagementId: String? = null
)
data class InventoryOutManagementFilter(
    var outManagementId: String? = null,
    var outType: String? = null,
    var factoryName: String? = null,
    var warehouseName: String? = null,
    var createUser: String? = null,
    var startDate: String? = null,
    var endDate: String? = null,
)
data class InventoryOutManagementSaveInput(
    var outType: String,
    var factoryId: String? = null,
    var warehouseId: String? = null,
    var totalPrice: String? = null,
)
data class InventoryOutSaveInput(
    val outManagementId: String,
    val supplierName: String? = null,
    val manufactureName: String? = null,
    val outType: String? = null,
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
data class InventoryOutUpdateInput(
    val outManagementId: String,
    val outInventoryId: String,
    val supplierName: String? = null,
    val manufactureName: String? = null,
    val outType: String? = null,
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
data class InventoryOutManagementDeleteInput (
    var outManagementId: String,
)
data class InventoryOutDeleteInput (
    var outInventoryId: String,
)
