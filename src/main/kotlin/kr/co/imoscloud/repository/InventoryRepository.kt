package kr.co.imoscloud.repository

import kr.co.imoscloud.entity.Inventory.InventoryInM
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
        select new kr.co.imoscloud.entity.Inventory.InventoryInM(
            iim.id,
            iim.site,
            iim.compCd,
            iim.factoryId,
            iim.warehouseId,
            iim.totalPrice,
            iim.hasInvoice,
            iim.remarks,
            null,
            iim.createUser,
            iim.createDate,
            iim.updateUser,
            iim.updateDate,
            iim.inManagementId
        )
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