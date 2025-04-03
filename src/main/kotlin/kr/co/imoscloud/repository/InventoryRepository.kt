package kr.co.imoscloud.repository

import kr.co.imoscloud.entity.inventory.InventoryIn
import kr.co.imoscloud.entity.inventory.InventoryInM
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface InventoryInMRep : JpaRepository<InventoryInM, Long>{
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
        and iim.factoryId = :factoryId
        and iim.flagActive = true
    """)
    fun getInventoryList(
        site: String,
        compCd: String,
        warehouseId: String,
        factoryId:String,
//        factoryName:String,
//        factoryCode:String,
//        flagActive:Boolean?
    ): List<InventoryInM?>

    @Query(
        value = """
        select
            ii.IN_MANAGEMENT_ID as "IN_MANAGEMENT_ID",
            ii.IN_INVENTORY_ID as "IN_INVENTORY_ID",
            mm.SUPPLIER_NAME as "SUPPLIER_NAME",
            mm.MANUFACTURER_NAME as "MANUFACTURER_NAME",
            mm.USER_MATERIAL_ID as "USER_MATERIAL_ID",
            mm.MATERIAL_NAME as "MATERIAL_NAME",
            mm.MATERIAL_CATEGORY as "MATERIAL_CATEGORY",
            mm.MATERIAL_STANDARD as "MATERIAL_STANDARD",
            ii.QTY as "QTY",
            ii.UNIT_PRICE as "UNIT_PRICE",
            ii.UNIT_VAT as "UNIT_VAT",
            ii.TOTAL_PRICE as "TOTAL_PRICE",
            ii.CREATE_USER as "CREATE_USER",
            ii.CREATE_DATE as "CREATE_DATE",
            ii.UPDATE_USER as "UPDATE_USER",
            ii.UPDATE_DATE as "UPDATE_DATE"
        from INVENTORY_IN ii
        left join MATERIAL_MASTER mm on ii.SYSTEM_MATERIAL_ID = mm.SYSTEM_MATERIAL_ID
        where 1=1
            and ii.SITE = :site
            and ii.COMP_CD = :compCd
            and ii.IN_MANAGEMENT_ID = :inManagementId
            and ii.FLAG_ACTIVE = true
        """, nativeQuery = true)
    fun getDetailedInventoryList(
        @Param("inManagementId") inManagementId: String,
        @Param("site") site: String,
        @Param("compCd") compCd: String,
    ): List<Map<String, Any>>

    // 가장 최근의 IN_MANAGEMENT_ID
    fun findTopByOrderByInManagementIdDesc(): InventoryInM?

    fun deleteByInManagementIdAndSiteAndCompCd(
        inManagementId: String,
        site: String,
        compCd: String
    )
}

interface InventoryInRep : JpaRepository<InventoryIn, Long> {

    fun findTopByOrderByInManagementIdDesc(): InventoryIn?

    // 자식노드 cascade 시 사용
    fun deleteByInManagementIdAndSiteAndCompCd(
        inManagementId: String,
        site: String,
        compCd: String
    )
}



