package kr.co.imoscloud.fetcher.inventory

import kr.co.imoscloud.service.InventoryInMResponseModel
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
    fun getInventoryList(@InputArgument("filter") filter: InventoryInMFilter): List<InventoryInMResponseModel?> {
        return inventoryService.getInventoryList(filter)
    }

    @DgsQuery
    fun getDetailedInventoryList(@InputArgument("filter") filter: InventoryInFilter): List<InventoryInResponseModel?> {
        return inventoryService.getDetailedInventoryList(filter)
    }

    @DgsMutation
    fun saveDetailedInventory(@InputArgument("createdRows") createdRows: List<DetailedInventoryInput?>,
//        @InputArgument("updatedRows") updatedRows:List<DetailedInventoryUpdate?>
    ): Boolean { inventoryService.saveDetailedInventory(createdRows)
        return true
    }

    @DgsMutation
    fun saveInventory(@InputArgument("createdRows") createdRows: List<InventoryInMInput?>,
    ): Boolean { inventoryService.saveInventory(createdRows)
        return true
    }

    @DgsQuery
    fun testString(): String {
        println("테스트 쿼리 호출됨")
        return "테스트가 성공했습니다!"
    }
}

data class InventoryInFilter(
    val site: String,
    val compCd: String,
    val inManagementId: String? = null
)

data class DetailedInventoryInput(
    val inManagementId: String,
    val supplierName: String? = null,
    val manufactureName: String? = null,
    val userMaterialId: String? = null,
    val inType: String? = null,
    val systemMaterialId: String? = null,
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

data class DetailedInventoryUpdate(
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

data class InventoryInMFilter(
    var site: String? = null,
    var compCd: String? = null,
    var factoryId: String? = null,
    var warehouseId: String? = null,
)

data class InventoryInMInput(
    var site: String,
    var compCd: String,
    var factoryId: String? = null,
    var warehouseId: String? = null,
    var totalPrice: String? = null,
    var hasInvoice: String? = null,
)
