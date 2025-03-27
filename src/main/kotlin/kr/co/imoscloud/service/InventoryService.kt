package kr.co.imoscloud.service

import kr.co.imoscloud.entity.Inventory.InventoryInMFilter
import kr.co.imoscloud.repository.InventoryRep
import org.springframework.stereotype.Service

@Service
class InventoryService (
    val inventoryRep: InventoryRep,
){
    fun getInventoryList(filter: InventoryInMFilter): List<InventoryInMResponseModel?> {
        val inventoryList = inventoryRep.getInventoryList(
            site = filter.site ?: "imos",
            compCd = filter.compCd ?: "eightPin",
            warehouseId = filter.warehouseId ?: "FTY-001"
        )

        val result = inventoryList.map {
            InventoryInMResponseModel(
                inManagementId = it?.inManagementId,
                factoryId = it?.factoryId,
                warehouseId = it?.warehouseId,
                totalPrice = it?.totalPrice.toString(),
                hasInvoice = it?.hasInvoice.toString(),
                createDate = it?.createDate.toString(),
            )
        }

        return result
    }
}

data class InventoryInMResponseModel(
    val inManagementId: String? = null,
    val factoryId: String? = null,
    val warehouseId: String? = null,
    val totalPrice: String? = null,
    val hasInvoice: String? = null,
    val createDate: String? = null,
    val id: String? = null,
    val site: String? = null,
    val compCd: String? = null,
    val remarks: String? = null,
    val createUser: String? = null,
    val updateUser: String? = null,
    val updateDate: String? = null
)

