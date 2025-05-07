package kr.co.imoscloud.service.standardInfo

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.standardInfo.Warehouse
import kr.co.imoscloud.fetcher.standardInfo.WareHouseInput
import kr.co.imoscloud.fetcher.standardInfo.WarehouseFilter
import kr.co.imoscloud.fetcher.standardInfo.WarehouseUpdate
import kr.co.imoscloud.repository.WarehouseRep
import kr.co.imoscloud.security.UserPrincipal
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class WarehouseService(
    val warehouseRep: WarehouseRep
) {
    fun getWarehouse(filter:WarehouseFilter): List<WarehouseResponse?> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        return warehouseRep.getWarehouses(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            factoryId = filter.factoryId,
            factoryName = filter.factoryName,
            warehouseId = filter.warehouseId,
            warehouseName = filter.warehouseName,
            warehouseType = filter.warehouseType,
        )
    }

    @Transactional
    fun saveWarehouse(createdRows:List<WareHouseInput?>, updatedRows:List<WarehouseUpdate?>){
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        createdRows.filterNotNull().takeIf { it.isNotEmpty() }?.let {createWarehouse(it, userPrincipal)}
        updatedRows.filterNotNull().takeIf { it.isNotEmpty() }?.let {updateWarehouse(it, userPrincipal)}
    }

    private fun createWarehouse(createdRows: List<WareHouseInput>, userPrincipal: UserPrincipal) {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val warehouseList = createdRows.map {
            Warehouse(
                site = userPrincipal.getSite(),
                compCd = userPrincipal.compCd,
                factoryId = it.factoryId,
                warehouseId = "W" + LocalDateTime.now().format(formatter) +
                        System.nanoTime().toString().takeLast(3),
                warehouseName = it.warehouseName,
                warehouseType = it.warehouseType,
            ).apply{
                createCommonCol(userPrincipal)
            }
        }

        warehouseRep.saveAll(warehouseList)
    }

    private fun updateWarehouse(updatedRows: List<WarehouseUpdate>, userPrincipal: UserPrincipal) {
        val warehouseIds = updatedRows.map {
            it.warehouseId
        }

        val warehouseList = warehouseRep.getWarehouseListByIds(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
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
                it.updateCommonCol(userPrincipal)
            }
        }

        warehouseRep.saveAll(warehouseList)

    }

    fun deleteWarehouse(warehouseId:String): Boolean {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        return warehouseRep.deleteByWarehouseId(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            warehouseId = warehouseId,
            updateUser = userPrincipal.loginId
        ) > 0
    }

    fun getWarehouse(): List<WarehouseResponse?> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        return warehouseRep.getGridWarehouse(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
        )
    }

}

data class WarehouseResponse(
    val factoryId: String,
    val factoryName: String?,
    val warehouseId: String,
    val warehouseName: String,
    val warehouseType: String,
//    val flagActive: String,
    val createUser: String?= null,
    val createDate: LocalDateTime? = null,
    val updateUser: String? = null,
    val updateDate: LocalDateTime? = null,
)