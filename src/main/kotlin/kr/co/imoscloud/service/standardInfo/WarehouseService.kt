package kr.co.imoscloud.service.standardInfo

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.standardInfo.Warehouse
import kr.co.imoscloud.fetcher.standardInfo.WareHouseInput
import kr.co.imoscloud.fetcher.standardInfo.WarehouseFilter
import kr.co.imoscloud.fetcher.standardInfo.WarehouseUpdate
import kr.co.imoscloud.repository.WarehouseRep
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class WarehouseService(
    val warehouseRep: WarehouseRep
) {

    fun getWarehouse(filter:WarehouseFilter): List<WarehouseResponse?> {
        return warehouseRep.getWarehouses(
            site = "imos",
            compCd = "eightPin",
            factoryId = filter.factoryId,
            factoryName = filter.factoryName,
            warehouseId = filter.warehouseId,
            warehouseName = filter.warehouseName,
            flagActive = filter.flagActive?.let{ it == "Y" }
        )
    }

    @Transactional
    fun saveWarehouse(createdRows:List<WareHouseInput?>, updatedRows:List<WarehouseUpdate?>){
        createdRows.filterNotNull().takeIf { it.isNotEmpty() }?.let {createWarehouse(it)}
        updatedRows.filterNotNull().takeIf { it.isNotEmpty() }?.let {updateWarehouse(it)}
    }

    private fun createWarehouse(createdRows: List<WareHouseInput>){
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val warehouseList = createdRows.map {
            Warehouse(
                site = "imos",
                compCd = "eightPin",
                factoryId = it.factoryId,
                warehouseId = "W" + LocalDateTime.now().format(formatter) +
                        System.nanoTime().toString().takeLast(3),
                warehouseName = it.warehouseName,
                warehouseType = it.warehouseType,
                flagActive = it.flagActive.equals("Y" ),
            )
        }

        warehouseRep.saveAll(warehouseList)
    }

    private fun updateWarehouse(updatedRows: List<WarehouseUpdate>){
        val warehouseIds = updatedRows.map {
            it.warehouseId
        }

        val warehouseList = warehouseRep.getWarehouseListByIds(
            site = "imos",
            compCd = "eightPin",
            warehouseIds = warehouseIds,
        )

        val updateList = warehouseList.associateBy { it?.warehouseId }

        updatedRows.forEach{ x ->
            val warehouseId = x.warehouseId
            val warehouse = updateList[warehouseId]

            warehouse?.let{
                it.factoryId = x.factoryId
                it.warehouseName = x.warehouseName
                it.warehouseType = x.warehouseType
                it.flagActive = x.flagActive.equals("Y" )
            }
        }

        warehouseRep.saveAll(warehouseList)

    }

    fun deleteWarehouse(warehouseId:String): Boolean {
        return warehouseRep.deleteByWarehouseId(
            site = "imos",
            compCd = "eightPin",
            warehouseId = warehouseId
        ) > 0
    }

}

data class WarehouseResponse(
    val factoryId: String,
    val factoryName: String,
    val warehouseId: String,
    val warehouseName: String,
    val warehouseType: String,
    val flagActive: String,
    val createUser: String?= null,
    val createDate: LocalDate? = null,
    val updateUser: String? = null,
    val updateDate: LocalDate? = null,
)