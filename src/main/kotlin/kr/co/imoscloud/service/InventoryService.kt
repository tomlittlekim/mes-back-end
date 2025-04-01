package kr.co.imoscloud.service

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.inventory.InventoryIn
import kr.co.imoscloud.fetcher.inventory.DetailedInventoryInput
import kr.co.imoscloud.fetcher.inventory.InventoryInFilter
import kr.co.imoscloud.fetcher.inventory.InventoryInMFilter
import kr.co.imoscloud.fetcher.inventory.InventoryInMInput
import kr.co.imoscloud.repository.InventoryInMRep
import kr.co.imoscloud.repository.InventoryInRep
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class InventoryService (
    val inventoryInMRep: InventoryInMRep,
    val inventoryInRep: InventoryInRep,
){

    /*
    * TODO:안 - 공통 - site,compCd 는 아마 유저정보에서 가져오는걸로
    * inType -> 드롭다운 방식으로 수정
    * system_material_id -> material 테이블에서 가져옴 */

    fun getInventoryList(filter: InventoryInMFilter): List<InventoryInMResponseModel?> {
        val inventoryList = inventoryInMRep.getInventoryList(
            site = filter.site ?: "imos",
            compCd = filter.compCd ?: "eightPin",
            warehouseId = filter.warehouseId ?: "WH-001",
            factoryId = filter.factoryId ?: "FTY-001",
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

    fun getDetailedInventoryList(filter: InventoryInFilter): List<InventoryInResponseModel?> {
        val detailedInventoryList = inventoryInMRep.getDetailedInventoryList(
            site = filter.site ?: "imos",
            compCd = filter.compCd ?: "eightPin",
            inManagementId = filter.inManagementId ?: throw IllegalArgumentException("입고관리번호는 필수입니다.")
        )

        return detailedInventoryList.map { result ->
            InventoryInResponseModel(
                inManagementId = result["IN_MANAGEMENT_ID"] as String?,
                inInventoryId = result["IN_INVENTORY_ID"] as String?,
                supplierName = result["SUPPLIER_NAME"] as String?,
                manufacturerName = result["MANUFACTURER_NAME"] as String?,
                userMaterialId = result["USER_MATERIAL_ID"] as String?,
                materialName = result["MATERIAL_NAME"] as String?,
                materialCategory = result["MATERIAL_CATEGORY"] as String?,
                materialStandard = result["MATERIAL_STANDARD"] as String?,
                qty = result["QTY"]?.toString(),
                unitPrice = result["UNIT_PRICE"]?.toString(),
                unitVat = result["UNIT_VAT"]?.toString(),
                totalPrice = result["TOTAL_PRICE"]?.toString(),
                createUser = result["CREATE_USER"] as String?,
                createDate = result["CREATE_DATE"]?.toString(),
                updateUser = result["UPDATE_USER"] as String?,
                updateDate = result["UPDATE_DATE"]?.toString()
            )
        }
    }

    @Transactional
    fun saveDetailedInventory(
        createdRows: List<DetailedInventoryInput?>,
//        updatedRows:List<DetailedInventoryUpdate?>
    ){
        createDetailedInventory(createdRows)
//        updateFactory(updatedRows)
    }

    @Transactional
    fun saveInventory(
        createdRows: List<InventoryInMInput?>,
    ){
    }

    fun createDetailedInventory(createdRows: List<DetailedInventoryInput?>){
        // 1. 마지막 입고 번호 조회
        val lastInManagementId = inventoryInRep.findTopByOrderByInManagementIdDesc()?.inManagementId

        // 2. 다음 입고 번호 생성
        val nextInManagementBase = if (lastInManagementId == null) {
            1
        } else {
            lastInManagementId.removePrefix("IN").toLongOrNull()?.plus(1) ?: 1
        }

        // 3. inventoryInList 생성
        val now = LocalDate.now()
        val inventoryInList = createdRows.mapIndexed { index, it ->
            InventoryIn().apply {
                site = "imos"
                compCd = "eightPin"
                inManagementId = it?.inManagementId ?: "IN${nextInManagementBase + index}"
                systemMaterialId = it?.systemMaterialId
                inType = it?.inType
                qty = it?.qty?.toInt()
                unitPrice = it?.unitPrice?.toInt()
                unitVat = it?.unitVat?.toInt()
                totalPrice = it?.totalPrice?.toInt()
                flagActive = true
                createUser = "admin"
                createDate = now
                updateUser = "admin"
                updateDate = now
                inInventoryId = UUID.randomUUID().toString()
            }
        }

        inventoryInRep.saveAll(inventoryInList)
    }

//    fun updateFactory(updatedRows: List<FactoryUpdate?>){
//        val factoryListId = updatedRows.map {
//            it?.factoryId
//        }
//
//        val factoryList = factoryRep.getFactoryListByIds(
//            site = "imos",
//            compCd = "eightPin",
//            factoryIds = factoryListId
//        )
//
//        val updateList = factoryList.associateBy { it?.factoryId }
//
//        updatedRows.forEach{ x ->
//            val factoryId = x?.factoryId
//            val factory = updateList[factoryId]
//
//            factory?.let{
//                it.factoryName = x?.factoryName
//                it.factoryCode = x?.factoryCode
//                it.address = x?.address
//                it.telNo = x?.telNo
//                it.officerName = x?.officerName
//                it.flagActive = x?.flagActive.equals("Y" )
//            }
//        }
//
//        factoryRep.saveAll(factoryList)
//    }
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

data class InventoryInResponseModel(
    val inManagementId: String? = null,
    val inInventoryId: String? = null,
    val supplierName: String? = null,
    val manufacturerName: String? = null,
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