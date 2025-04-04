package kr.co.imoscloud.repository

import kr.co.imoscloud.entity.inventory.InventoryIn
import kr.co.imoscloud.entity.inventory.InventoryInManagement
import kr.co.imoscloud.entity.inventory.InventoryOut
import kr.co.imoscloud.entity.inventory.InventoryOutManagement
import kr.co.imoscloud.service.InventoryInManagementResponseModel
import kr.co.imoscloud.service.InventoryInResponseModel
import kr.co.imoscloud.service.InventoryOutManagementResponseModel
import kr.co.imoscloud.service.InventoryOutResponseModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface InventoryInManagementRep : JpaRepository<InventoryInManagement, Long>{
    @Query(
        nativeQuery = true,
        value = """
                with material_info as (
                    select
                        in2.IN_MANAGEMENT_ID,
                        mm2.MATERIAL_NAME,
                        count(*) over (partition by in2.IN_MANAGEMENT_ID) as material_count,
                        row_number() over (partition by in2.IN_MANAGEMENT_ID order by in2.SEQ) as rn
                    from INVENTORY_IN in2
                             join MATERIAL_MASTER mm2 on mm2.SYSTEM_MATERIAL_ID = in2.SYSTEM_MATERIAL_ID
                )
                select
                    iim.IN_MANAGEMENT_ID as inManagementId,
                    iim.IN_TYPE as inType,
                    f.FACTORY_NAME as factoryName,
                    w.WAREHOUSE_NAME as warehouseName,
                    case
                        when mi.material_count > 1 then concat(mi.MATERIAL_NAME, ' 외 ', mi.material_count - 1, '건')
                        else mi.MATERIAL_NAME
                    end as materialInfo,
                    iim.TOTAL_PRICE as totalPrice,
                    iim.HAS_INVOICE as hasInvoice,
                    u.USER_NAME as userName,
                    DATE_FORMAT(iim.CREATE_DATE, '%Y-%m-%d') as createDate
                from INVENTORY_IN_MANAGEMENT iim
                         left join WAREHOUSE w on iim.WAREHOUSE_ID = w.WAREHOUSE_ID
                         left join FACTORY f on f.FACTORY_ID = iim.FACTORY_ID
                         left join USER u on u.LOGIN_ID = iim.CREATE_USER
                         left join material_info mi on mi.IN_MANAGEMENT_ID = iim.IN_MANAGEMENT_ID and mi.rn = 1
                where 1=1
                  and iim.IN_MANAGEMENT_ID like concat('%', :inManagementId, '%')
                  and iim.IN_TYPE like concat('%', :inType, '%')
                  and f.FACTORY_NAME like concat('%', :factoryName, '%')
                  and w.WAREHOUSE_NAME like concat('%', :warehouseName, '%')
                  and iim.CREATE_USER like concat('%', :createUser, '%')
                  and (:hasInvoice IS NULL OR :hasInvoice = '' 
                       OR (:hasInvoice = 'Y' AND iim.HAS_INVOICE IS NOT NULL AND iim.HAS_INVOICE <> '')
                       OR (:hasInvoice = 'N' AND iim.HAS_INVOICE IS NULL))
                  and (:startDate IS NULL OR :startDate = '' OR iim.CREATE_DATE >= STR_TO_DATE(:startDate, '%Y-%m-%d'))
                  and (:endDate IS NULL OR :endDate = '' OR iim.CREATE_DATE <= STR_TO_DATE(:endDate, '%Y-%m-%d'))
                  and iim.SITE = :site
                  and iim.COMP_CD = :compCd
                  and iim.FLAG_ACTIVE = :flagActive
            """
    )
    fun findInventoryInManagementWithMaterialInfo(
        @Param("inManagementId") inManagementId: String?,
        @Param("inType") inType: String?,
        @Param("factoryName") factoryName: String?,
        @Param("warehouseName") warehouseName: String?,
        @Param("createUser") createUser: String?,
        @Param("hasInvoice") hasInvoice: String?, // "Y" or "N" or null
        @Param("startDate") startDate: String?,
        @Param("endDate") endDate: String?,
        @Param("site") site: String,
        @Param("compCd") compCd: String,
        @Param("flagActive") flagActive: Boolean,
    ): List<InventoryInManagementResponseModel?>

    // 가장 최근의 IN_MANAGEMENT_ID
    fun findTopByOrderByInManagementIdDesc(): InventoryInManagement?

    fun deleteByInManagementIdAndSiteAndCompCd(
        inManagementId: String,
        site: String,
        compCd: String
    )
}

interface InventoryInRep : JpaRepository<InventoryIn, Long> {

    @Query(
        value = """
        select
            ii.IN_MANAGEMENT_ID as inManagementId,
            ii.IN_INVENTORY_ID as inInventoryId,
            mm.SUPPLIER_NAME as supplierName,
            mm.MANUFACTURER_NAME as manufacturerName,
            mm.USER_MATERIAL_ID as userMaterialId,
            mm.MATERIAL_NAME as materialName,
            mm.MATERIAL_CATEGORY as materialCategory,
            mm.MATERIAL_STANDARD as materialStandard,
            CAST(ii.QTY AS CHAR) as qty,
            CAST(ii.UNIT_PRICE AS CHAR) as unitPrice,
            CAST(ii.UNIT_VAT AS CHAR) as unitVat,
            CAST(ii.TOTAL_PRICE AS CHAR) as totalPrice,
            u1.USER_NAME as createUser,
            DATE_FORMAT(ii.CREATE_DATE, '%Y-%m-%d') as createDate,
            u2.USER_NAME as updateUser,
            DATE_FORMAT(ii.UPDATE_DATE, '%Y-%m-%d') as updateDate
        from INVENTORY_IN ii
        left join MATERIAL_MASTER mm on ii.SYSTEM_MATERIAL_ID = mm.SYSTEM_MATERIAL_ID
        left join USER u1 on u1.LOGIN_ID = ii.CREATE_USER
        left join USER u2 on u2.LOGIN_ID = ii.UPDATE_USER
        where 1=1
            and ii.SITE = :site
            and ii.COMP_CD = :compCd
            and ii.IN_MANAGEMENT_ID = :inManagementId
            and ii.FLAG_ACTIVE = true
        """, nativeQuery = true)
    fun findInventoryInWithMaterial(
        @Param("inManagementId") inManagementId: String,
        @Param("site") site: String,
        @Param("compCd") compCd: String,
    ): List<InventoryInResponseModel?>

    fun findTopByOrderByInManagementIdDesc(): InventoryIn?

    // 자식노드 cascade 시 사용
    fun deleteByInManagementIdAndSiteAndCompCd(
        inManagementId: String,
        site: String,
        compCd: String
    )

    fun deleteByInInventoryIdAndSiteAndCompCd(
        inInventoryId: String,
        site: String,
        compCd: String
    )



    @Query(
        value = """
            select f
            from InventoryIn f
            where f.site = :site
            and   f.compCd = :compCd
            and   f.inInventoryId IN (:inInventoryId)
        """
    )
    fun getDetailedInventoryInListByIds(
        site:String,
        compCd:String,
        inInventoryId:List<String?>
    ):List<InventoryIn?>
}

interface InventoryOutManagementRep : JpaRepository<InventoryOutManagement, Long> {
    @Query(
        nativeQuery = true,
        value = """
                with material_info as (
                    select
                        out2.OUT_MANAGEMENT_ID,
                        mm2.MATERIAL_NAME,
                        count(*) over (partition by out2.OUT_MANAGEMENT_ID) as material_count,
                        row_number() over (partition by out2.OUT_MANAGEMENT_ID order by out2.SEQ) as rn
                    from INVENTORY_OUT out2
                             join MATERIAL_MASTER mm2 on mm2.SYSTEM_MATERIAL_ID = out2.SYSTEM_MATERIAL_ID
                )
                select
                    iom.OUT_MANAGEMENT_ID as outManagementId,
                    iom.OUT_TYPE as outType,
                    f.FACTORY_NAME as factoryName,
                    w.WAREHOUSE_NAME as warehouseName,
                    case
                        when mi.material_count > 1 then concat(mi.MATERIAL_NAME, ' 외 ', mi.material_count - 1, '건')
                        else mi.MATERIAL_NAME
                    end as materialInfo,
                    iom.TOTAL_PRICE as totalPrice,
                    u.USER_NAME as userName,
                    DATE_FORMAT(iom.CREATE_DATE, '%Y-%m-%d') as createDate
                from INVENTORY_OUT_MANAGEMENT iom
                         left join WAREHOUSE w on iom.WAREHOUSE_ID = w.WAREHOUSE_ID
                         left join FACTORY f on f.FACTORY_ID = iom.FACTORY_ID
                         left join USER u on u.LOGIN_ID = iom.CREATE_USER
                         left join material_info mi on mi.OUT_MANAGEMENT_ID = iom.OUT_MANAGEMENT_ID and mi.rn = 1
                where 1=1
                  and iom.OUT_MANAGEMENT_ID like concat('%', :outManagementId, '%')
                  and iom.OUT_TYPE like concat('%', :outType, '%')
                  and f.FACTORY_NAME like concat('%', :factoryName, '%')
                  and w.WAREHOUSE_NAME like concat('%', :warehouseName, '%')
                  and iom.CREATE_USER like concat('%', :createUser, '%')
                  and (:startDate IS NULL OR :startDate = '' OR iom.CREATE_DATE >= STR_TO_DATE(:startDate, '%Y-%m-%d'))
                  and (:endDate IS NULL OR :endDate = '' OR iom.CREATE_DATE <= STR_TO_DATE(:endDate, '%Y-%m-%d'))
                  and iom.SITE = :site
                  and iom.COMP_CD = :compCd
                  and iom.FLAG_ACTIVE = :flagActive
            """
    )
    fun findInventoryOutManagementWithMaterialInfo(
        @Param("outManagementId") outManagementId: String?,
        @Param("outType") outType: String?,
        @Param("factoryName") factoryName: String?,
        @Param("warehouseName") warehouseName: String?,
        @Param("createUser") createUser: String?,
        @Param("startDate") startDate: String?,
        @Param("endDate") endDate: String?,
        @Param("site") site: String,
        @Param("compCd") compCd: String,
        @Param("flagActive") flagActive: Boolean,
    ): List<InventoryOutManagementResponseModel?>
}

interface InventoryOutRep : JpaRepository<InventoryOut, Long> {

    @Query(
        value = """
        select
            io.OUT_MANAGEMENT_ID as outManagementId,
            io.OUT_INVENTORY_ID as outInventoryId,
            mm.SUPPLIER_NAME as supplierName,
            mm.MANUFACTURER_NAME as manufacturerName,
            mm.USER_MATERIAL_ID as userMaterialId,
            mm.MATERIAL_NAME as materialName,
            mm.MATERIAL_CATEGORY as materialCategory,
            mm.MATERIAL_STANDARD as materialStandard,
            CAST(io.QTY AS CHAR) as qty,
            CAST(io.UNIT_PRICE AS CHAR) as unitPrice,
            CAST(io.UNIT_VAT AS CHAR) as unitVat,
            CAST(io.TOTAL_PRICE AS CHAR) as totalPrice,
            u1.USER_NAME as createUser,
            DATE_FORMAT(io.CREATE_DATE, '%Y-%m-%d') as createDate,
            u2.USER_NAME as updateUser,
            DATE_FORMAT(io.UPDATE_DATE, '%Y-%m-%d') as updateDate
        from INVENTORY_OUT io
        left join MATERIAL_MASTER mm on io.SYSTEM_MATERIAL_ID = mm.SYSTEM_MATERIAL_ID
        left join USER u1 on u1.LOGIN_ID = io.CREATE_USER
        left join USER u2 on u2.LOGIN_ID = io.UPDATE_USER
        where 1=1
            and io.SITE = :site
            and io.COMP_CD = :compCd
            and io.OUT_MANAGEMENT_ID = :outManagementId
            and io.FLAG_ACTIVE = true
        """, nativeQuery = true)
    fun findInventoryOutWithMaterial(
        @Param("outManagementId") outManagementId: String,
        @Param("site") site: String,
        @Param("compCd") compCd: String,
    ): List<InventoryOutResponseModel?>
}



