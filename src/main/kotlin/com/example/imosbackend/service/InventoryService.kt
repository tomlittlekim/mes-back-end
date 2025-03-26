package com.example.imosbackend.service

import com.example.imosbackend.entity.Inventory.InventoryInM
import com.example.imosbackend.entity.Inventory.InventoryInMFilter
import com.example.imosbackend.repository.InventoryRep
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class InventoryService (
    val inventoryRep: InventoryRep,
){
    fun getInventoryList(filter: InventoryInMFilter): List<InventoryInMResponseModel?> {
        val inventoryList = inventoryRep.getInventoryList(
            site = "imos",
            compCd = "eightPin",
            warehouseId = "FTY-001",
        )

        val result = inventoryList.map {
            InventoryInMResponseModel(
                inManagementId: String? = null,
                // 입고형태
                factoryId: String? = null, //
                warehouseId: String? = null, /
                totalPrice: String? = null, //
                hasInvoice: String? = null, //
            )
        }

    }
}

data class InventoryInMResponseModel(
    val id: String? = null, // SEQ
    val site: String? = null, // SITE
    val compCd: String? = null, // COMP_CD
    val factoryId: String? = null, // FACTORY_ID
    val warehouseId: String? = null, // WAREHOUSE_ID
    val totalPrice: String? = null, // TOTAL_PRICE
    val hasInvoice: String? = null, // HAS_INVOICE
    val remarks: String? = null, // REMARKS
    val flagActive: String? = null, // FLAG_ACTIVE
    val createUser: String? = null, // CREATE_USER
    val createDate: String? = null, // CREATE_DATE
    val updateUser: String? = null, // UPDATE_USER
    val updateDate: String? = null, // UPDATE_DATE
    val inManagementId: String? = null // IN_MANAGEMENT_ID
)

