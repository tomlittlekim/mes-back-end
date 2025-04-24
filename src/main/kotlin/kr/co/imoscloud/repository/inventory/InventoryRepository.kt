package kr.co.imoscloud.repository.inventory

import kr.co.imoscloud.entity.inventory.*
import kr.co.imoscloud.entity.standardInfo.Warehouse
import kr.co.imoscloud.service.inventory.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime


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
            iim.FACTORY_ID as factoryId,
            iim.WAREHOUSE_ID as warehouseId,
            case
                when mi.material_count > 1 then concat(mi.MATERIAL_NAME, ' 외 ', mi.material_count - 1, '건')
                else mi.MATERIAL_NAME
            end as materialInfo,
            iim.TOTAL_PRICE as totalPrice,
            iim.HAS_INVOICE as hasInvoice,
            u.USER_NAME as userName,
            DATE_FORMAT(iim.CREATE_DATE, '%Y-%m-%d %H:%i:%s') as createDate
        from INVENTORY_IN_MANAGEMENT iim
        left join WAREHOUSE w on w.WAREHOUSE_ID = iim.WAREHOUSE_ID
            and w.COMP_CD = iim.COMP_CD and w.SITE = iim.SITE
        left join FACTORY f on f.FACTORY_ID = iim.FACTORY_ID
            and f.COMP_CD = iim.COMP_CD and f.SITE = iim.SITE
        left join USER u on u.LOGIN_ID = iim.CREATE_USER
            and u.COMP_CD = iim.COMP_CD and u.SITE = iim.SITE
        left join material_info mi on mi.IN_MANAGEMENT_ID = iim.IN_MANAGEMENT_ID and mi.rn = 1
        where iim.SITE = :site
          and iim.COMP_CD = :compCd
          and iim.FLAG_ACTIVE = :flagActive
          and (:inManagementId IS NULL OR :inManagementId = '' OR iim.IN_MANAGEMENT_ID like concat('%', :inManagementId, '%'))
          and (:inType IS NULL OR :inType = '' OR iim.IN_TYPE like concat('%', :inType, '%'))
          and (:factoryName IS NULL OR :factoryName = '' OR f.FACTORY_NAME like concat('%', :factoryName, '%'))
          and (:warehouseName IS NULL OR :warehouseName = '' OR w.WAREHOUSE_NAME like concat('%', :warehouseName, '%'))
          and (:createUser IS NULL OR :createUser = '' OR iim.CREATE_USER like concat('%', :createUser, '%'))
          and (
            :hasInvoice is null or :hasInvoice = '' or
            (:hasInvoice = 'Y' and iim.HAS_INVOICE is not null and iim.HAS_INVOICE <> '') or
            (:hasInvoice = 'N' and (iim.HAS_INVOICE is null or iim.HAS_INVOICE = ''))
          )
          and (:startDate is null or :startDate = '' or iim.CREATE_DATE >= STR_TO_DATE(:startDate, '%Y-%m-%d'))
          and (:endDate is null or :endDate = '' or iim.CREATE_DATE <= STR_TO_DATE(:endDate, '%Y-%m-%d 23:59:59'))
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

    fun findByCompCdAndSiteAndInManagementId(
        compCd: String?,
        site: String?,
        inManagementId: String?
    ): InventoryInManagement?

}

interface InventoryInRep : JpaRepository<InventoryIn, Long> {

    @Query(
        value = """
        select
            ii.IN_MANAGEMENT_ID as inManagementId,
            ii.IN_INVENTORY_ID as inInventoryId,
            mm.SUPPLIER_NAME as supplierName,
            mm.MANUFACTURER_NAME as manufacturerName,
            mm.SYSTEM_MATERIAL_ID as systemMaterialId,
            mm.MATERIAL_NAME as materialName,
            mm.MATERIAL_CATEGORY as materialCategory,
            mm.MATERIAL_STANDARD as materialStandard,
            CAST(ii.QTY AS CHAR) as qty,
            CAST(ii.UNIT_PRICE AS CHAR) as unitPrice,
            CAST(ii.UNIT_VAT AS CHAR) as unitVat,
            CAST(ii.TOTAL_PRICE AS CHAR) as totalPrice,
            u1.USER_NAME as createUser,
            DATE_FORMAT(ii.CREATE_DATE, '%Y-%m-%d %H:%i:%s') as createDate,
            u2.USER_NAME as updateUser,
            DATE_FORMAT(ii.UPDATE_DATE, '%Y-%m-%d %H:%i:%s') as updateDate
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

    fun findTopByCompCdAndSiteAndSystemMaterialId(
        compCd: String?,
        site: String?,
        systemMaterialId: String?
    ): InventoryIn?

    fun findByInManagementIdAndSiteAndCompCd(
        inManagementId: String,
        site: String,
        compCd: String
    ): List<InventoryIn>

    fun findByInInventoryIdAndSiteAndCompCd(
        inInventoryId: String,
        site: String,
        compCd: String
    ): InventoryIn?

    @Query("SELECT SUM(i.totalPrice) FROM InventoryIn i WHERE i.site = :site AND i.compCd = :compCd AND i.inManagementId = :inManagementId AND i.flagActive = true")
    fun calculateTotalSumByInManagementId(@Param("site") site: String, @Param("compCd") compCd: String, @Param("inManagementId") inManagementId: String): Int?
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
                    iom.FACTORY_ID as factoryId,
                    iom.WAREHOUSE_ID as WAREHOUSE_ID,
                    case
                        when mi.material_count > 1 then concat(mi.MATERIAL_NAME, ' 외 ', mi.material_count - 1, '건')
                        else mi.MATERIAL_NAME
                    end as materialInfo,
                    iom.TOTAL_PRICE as totalPrice,
                    u.USER_NAME as userName,
                    DATE_FORMAT(iom.CREATE_DATE, '%Y-%m-%d %H:%i:%s') as createDate
                from INVENTORY_OUT_MANAGEMENT iom
                         left join WAREHOUSE w on iom.WAREHOUSE_ID = w.WAREHOUSE_ID
                         and w.COMP_CD = iom.COMP_CD
                         and w.SITE = iom.SITE
                         left join FACTORY f on f.FACTORY_ID = iom.FACTORY_ID
                         and f.COMP_CD = iom.COMP_CD
                         and f.SITE = iom.SITE
                         left join USER u on u.LOGIN_ID = iom.CREATE_USER
                         and u.COMP_CD = iom.COMP_CD
                         and u.SITE = iom.SITE
                         left join material_info mi on mi.OUT_MANAGEMENT_ID = iom.OUT_MANAGEMENT_ID and mi.rn = 1
                where 1=1
                  and (:outManagementId IS NULL OR :outManagementId = '' OR iom.OUT_MANAGEMENT_ID like concat('%', :outManagementId, '%'))
                  and (:outType IS NULL OR :outType = '' OR iom.OUT_TYPE like concat('%', :outType, '%'))
                  and (:factoryName IS NULL OR :factoryName = '' OR f.FACTORY_NAME like concat('%', :factoryName, '%'))
                  and (:warehouseName IS NULL OR :warehouseName = '' OR w.WAREHOUSE_NAME like concat('%', :warehouseName, '%'))
                  and (:createUser IS NULL OR :createUser = '' OR iom.CREATE_USER like concat('%', :createUser, '%'))
                  and (:startDate IS NULL OR :startDate = '' OR iom.CREATE_DATE >= STR_TO_DATE(:startDate, '%Y-%m-%d'))
                  and (:endDate IS NULL OR :endDate = '' OR iom.CREATE_DATE <= STR_TO_DATE(:endDate, '%Y-%m-%d 23:59:59'))
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
    
    // 가장 최근의 OUT_MANAGEMENT_ID
    fun findTopByOrderByOutManagementIdDesc(): InventoryOutManagement?

    fun deleteByOutManagementIdAndSiteAndCompCd(
        outManagementId: String,
        site: String,
        compCd: String
    )

    fun findByOutManagementIdAndSiteAndCompCd(
        outManagementId: String,
        site: String,
        compCd: String
    ): InventoryOutManagement?
}

interface InventoryOutRep : JpaRepository<InventoryOut, Long> {

    @Query(
        value = """
        select
            io.OUT_MANAGEMENT_ID as outManagementId,
            io.OUT_INVENTORY_ID as outInventoryId,
            mm.SUPPLIER_NAME as supplierName,
            mm.MANUFACTURER_NAME as manufacturerName,
            mm.SYSTEM_MATERIAL_ID as systemMaterialId,
            mm.MATERIAL_NAME as materialName,
            mm.MATERIAL_CATEGORY as materialCategory,
            mm.MATERIAL_STANDARD as materialStandard,
            CAST(io.QTY AS CHAR) as qty,
            CAST(io.UNIT_PRICE AS CHAR) as unitPrice,
            CAST(io.UNIT_VAT AS CHAR) as unitVat,
            CAST(io.TOTAL_PRICE AS CHAR) as totalPrice,
            u1.USER_NAME as createUser,
            DATE_FORMAT(io.CREATE_DATE, '%Y-%m-%d %H:%i:%s') as createDate,
            u2.USER_NAME as updateUser,
            DATE_FORMAT(io.UPDATE_DATE, '%Y-%m-%d %H:%i:%s') as updateDate
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
    
    fun findTopByOrderByOutManagementIdDesc(): InventoryOut?

    // 자식노드 cascade 시 사용
    fun deleteByOutManagementIdAndSiteAndCompCd(
        outManagementId: String,
        site: String,
        compCd: String
    )

    fun deleteByOutInventoryIdAndSiteAndCompCd(
        outInventoryId: String,
        site: String,
        compCd: String
    )

    @Query(
        value = """
            select f
            from InventoryOut f
            where f.site = :site
            and   f.compCd = :compCd
            and   f.outInventoryId IN (:outInventoryId)
        """
    )
    fun getDetailedInventoryOutListByIds(
        site:String,
        compCd:String,
        outInventoryId:List<String?>
    ):List<InventoryOut?>

    @Query(
        value = """
            SELECT f.FACTORY_ID
            FROM INVENTORY_OUT_MANAGEMENT o
            JOIN FACTORY f ON f.FACTORY_ID = o.FACTORY_ID
            WHERE o.OUT_MANAGEMENT_ID = :outManagementId
            AND o.SITE = :site
            AND o.COMP_CD = :compCd
            LIMIT 1
        """, nativeQuery = true
    )

    fun findByOutManagementIdAndSiteAndCompCd(
        outManagementId: String,
        site: String,
        compCd: String
    ): List<InventoryOut>

    fun findByOutInventoryIdAndSiteAndCompCd(
        outInventoryId: String,
        site: String,
        compCd: String
    ): InventoryOut?

    @Query("SELECT SUM(o.totalPrice) FROM InventoryOut o WHERE o.site = :site AND o.compCd = :compCd AND o.outManagementId = :outManagementId AND o.flagActive = true")
    fun calculateTotalSumByOutManagementId(@Param("site") site: String, @Param("compCd") compCd: String, @Param("outManagementId") outManagementId: String): Int?
}

interface InventoryStatusRep : JpaRepository<InventoryStatus, Long> {

    @Query("""
            with
            filtered_warehouse as (
                select * from WAREHOUSE where COMP_CD = :compCd and SITE = :site
            ),
            filtered_material_master as (
                select * from MATERIAL_MASTER where COMP_CD = :compCd and SITE = :site
            )
            select 
                fw.WAREHOUSE_NAME as warehouseName,
                fmm.SUPPLIER_NAME as supplierName,
                fmm.MANUFACTURER_NAME as manufacturerName,
                fmm.SYSTEM_MATERIAL_ID as systemMaterialId,
                fmm.MATERIAL_NAME as materialName,
                fmm.UNIT as unit,
                invs.QTY as qty
            from INVENTORY_STATUS invs
            left join filtered_material_master fmm on invs.SYSTEM_MATERIAL_ID = fmm.SYSTEM_MATERIAL_ID
            left join filtered_warehouse fw on invs.WAREHOUSE_ID = fw.WAREHOUSE_ID
            where 1=1
              and invs.SITE = :site
              and invs.COMP_CD = :compCd
              and (:warehouseName IS NULL OR :warehouseName = '' OR fw.WAREHOUSE_NAME like concat('%', :warehouseName, '%'))
              and (:supplierName IS NULL OR :supplierName = '' OR fmm.SUPPLIER_NAME like concat('%', :supplierName, '%'))
              and (:manufacturerName IS NULL OR :manufacturerName = '' OR fmm.MANUFACTURER_NAME like concat('%', :manufacturerName, '%'))
              and (:materialName IS NULL OR :materialName = '' OR fmm.MATERIAL_NAME like concat('%', :materialName, '%'))
        """, nativeQuery = true)
    fun findInventoryStatusFiltered(
        @Param("site") site: String?,
        @Param("compCd") compCd: String?,
        @Param("warehouseName") warehouseName: String?,
        @Param("supplierName") supplierName: String?,
        @Param("manufacturerName") manufacturerName: String?,
        @Param("materialName") materialName: String?
    ): List<InventoryStatusResponseModel?>

    fun findByCompCdAndSiteAndSystemMaterialIdIn(
        compCd: String,
        site: String,
        systemMaterialIds: List<String>
    ): List<InventoryStatus>

    fun findByCompCdAndSiteAndSystemMaterialId(
        compCd: String,
        site: String,
        systemMaterialId: String
    ): InventoryStatus?

    @Query("""
        select wh
        from InventoryStatus its
        left join Warehouse wh on its.warehouseId = wh.warehouseId
            and its.site = wh.site
            and its.compCd = wh.compCd
            and wh.flagActive is true
        where its.systemMaterialId = :systemMaterialId
            and its.site = :site
            and its.compCd = :compCd
            and its.qty > 0
            and its.flagActive is true
    """)
    fun getWarehouseByMaterialId(
        site: String,
        compCd: String,
        systemMaterialId: String
    ): List<Warehouse>

    @Modifying
    @Query("""
        update InventoryStatus its
        set
            its.qty = (its.qty + :qty), 
            its.updateDate = :updateDate,
            its.updateUser = :updateUser
        where its.compCd = :compCd
            and its.warehouseId = :warehouseId
            and its.systemMaterialId = :systemMaterialId
            and its.flagActive is true
    """)
    fun updateQtyByIdAndSystemMaterialId(
        compCd: String,
        warehouseId: String,
        systemMaterialId: String,
        qty: Double,
        updateUser: String,
        updateDate: LocalDateTime = LocalDateTime.now()
    ): Int
}

interface InventoryHistoryRep : JpaRepository<InventoryHistory, Long> {
    @Query("""
    SELECT ih FROM InventoryHistory ih
    WHERE ih.inOutType IN ('IN', 'OUT')
    AND (:site IS NULL OR ih.site = :site)
    AND (:compCd IS NULL OR ih.compCd = :compCd)
    AND (:warehouseName IS NULL OR ih.warehouseName LIKE CONCAT('%', :warehouseName, '%'))
    AND (:inOutType IS NULL OR ih.inOutType LIKE CONCAT('%', :inOutType, '%'))
    AND (:supplierName IS NULL OR ih.supplierName LIKE CONCAT('%', :supplierName, '%'))
    AND (:manufacturerName IS NULL OR ih.manufacturerName LIKE CONCAT('%', :manufacturerName, '%'))
    AND ((:materialNames) IS NULL OR ih.materialName IN :materialNames)
    AND (:startDate IS NULL OR ih.createDate >= FUNCTION('STR_TO_DATE', :startDate, '%Y-%m-%d'))
    AND (:endDate IS NULL OR ih.createDate <= FUNCTION('STR_TO_DATE', :endDate, '%Y-%m-%d'))
""")
    fun searchInventoryHistory(
        @Param("site") site: String?,
        @Param("compCd") compCd: String?,
        @Param("warehouseName") warehouseName: String?,
        @Param("inOutType") inOutType: String?,
        @Param("supplierName") supplierName: String?,
        @Param("manufacturerName") manufacturerName: String?,
        @Param("materialNames") materialNames: List<String>?,
        @Param("startDate") startDate: String?,
        @Param("endDate") endDate: String?
    ): List<InventoryHistory>
}



