package com.example.imosbackend.repository

import com.example.imosbackend.entity.Inventory.InventoryInM
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface InventoryRep : JpaRepository<InventoryInM, Long>{
//    @Query("""
//        select iim
//        from InventoryInM iim
//        where 1=1
//        and iim.site = :site
//        and iim.compCd = :compCd
//        and iim.warehouseId = :warehouseId
//        and (iim.factoryId like concat ('%',:factoryId,'%'))
//        and (iim.factoryName like concat ('%',:factoryName,'%'))
//        and (iim.factoryCode like concat ('%',:factoryCode,'%'))
//        and (:flagActive is null or f.flagActive = :flagActive)
//    """
//    )
@Query("""
        select iim
        from InventoryInM iim
        where 1=1
        and iim.site = :site
        and iim.compCd = :compCd
        and iim.warehouseId = :warehouseId
    """
)
    fun getInventoryList(
        site: String,
        compCd: String,
        warehouseId: String,
//        factoryId:String,
//        factoryName:String,
//        factoryCode:String,
//        flagActive:Boolean?
    ): List<InventoryInM?>
}