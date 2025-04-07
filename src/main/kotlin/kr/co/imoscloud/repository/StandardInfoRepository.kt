package kr.co.imoscloud.repository

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.standardInfo.*
import kr.co.imoscloud.service.standardInfo.EquipmentResponseModel
import kr.co.imoscloud.service.standardInfo.LineResponseModel
import kr.co.imoscloud.service.standardInfo.WarehouseResponse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface FactoryRep: JpaRepository<Factory,Long>{
    @Query(
        value = """
            select f
            from Factory f
            where f.site = :site
            and   f.compCd = :compCd
            and   (f.factoryId like concat ('%',:factoryId,'%'))
            and   (f.factoryName like concat ('%',:factoryName,'%'))
            and   (f.factoryCode like concat ('%',:factoryCode,'%'))
            and   (:flagActive is null or f.flagActive = :flagActive)
        """
    )
    fun getFactoryList(
        site:String,
        compCd:String,
        factoryId:String,
        factoryName:String,
        factoryCode:String,
        flagActive:Boolean?
    ):List<Factory?>

    @Query(
        value = """
            select f
            from Factory f
            where f.site = :site
            and   f.compCd = :compCd
            and   f.factoryId IN (:factoryIds)
        """
    )
    fun getFactoryListByIds(
        site:String,
        compCd:String,
        factoryIds:List<String?>
    ):List<Factory?>

    @Query(
        value = """
            select f
            from Factory f
            where f.site = :site
            and   f.compCd = :compCd
            and   f.flagActive = true
        """
    )
    fun getGridFactory(
        site:String,
        compCd:String
    ):List<Factory?>


    @Transactional
    @Modifying
    @Query("""
        delete 
        from Factory f 
        where f.factoryId = :factoryId
        and   f.site = :site
        and   f.compCd = :compCd
        """
    )
    fun deleteByFactoryId(
        site:String,
        compCd:String,
        factoryId: String
    ): Int
}

interface CodeClassRep : JpaRepository<CodeClass,Long>{
    @Query(
        value = """
            select cc
            from CodeClass cc
            where cc.site = :site
            and   cc.compCd = :compCd
            and   (cc.codeClassId like concat ('%',:codeClassId,'%'))
            and   (cc.codeClassName like concat ('%',:codeClassName,'%'))
        """
    )
    fun getCodeClassList(
        site:String,
        compCd:String,
        codeClassId:String,
        codeClassName:String
    ):List<CodeClass?>

    @Query(
        value = """
            select cc
            from CodeClass cc
            where cc.site = :site
            and   cc.compCd = :compCd
            and   cc.codeClassId IN (:codeClassIds)
        """
    )
    fun getCodeClassListByIds(
        site:String,
        compCd:String,
        codeClassIds:List<String?>
    ):List<CodeClass?>
}

interface CodeRep: JpaRepository<Code,Long>{

    @Query(
        value = """
            select c
            from Code c
            where c.site = :site
            and   c.compCd = :compCd
            and   c.codeClassId = :codeClassId
        """
    )
    fun getCodeList(
        site:String,
        compCd:String,
        codeClassId:String
    ):List<Code?>

    @Query(
        value = """
            select c
            from Code c
            where c.site = :site
            and   c.compCd = :compCd
            and   c.codeClassId = :codeClassId
            and   c.codeId IN (:codeIds)
        """
    )
    fun getCodeListByIds(
        site:String,
        compCd:String,
        codeClassId:String,
        codeIds:List<String?>
    ):List<Code?>


    @Transactional
    @Modifying
    @Query("""
        delete 
        from Code c 
        where c.site = :site
        and   c.compCd = :compCd
        and   c.codeId = :codeId
        """
    )
    fun deleteByCodeId(
        site:String,
        compCd:String,
        codeId: String
    ): Int

    @Query(
        value = """
            select c
            from Code c
            where c.site = :site
            and   c.compCd = :compCd
            and   c.codeClassId = :codeClassId
            and   c.flagActive = true
            order by c.sortOrder
        """
    )
    fun getGridCodes(
        site:String,
        compCd:String,
        codeClassId:String
    ):List<Code?>

    fun findAllByCodeClassIdIn(codeClassIds: List<String>): List<Code?>

    @Query("""
        select c
        from CodeClass c
        where c.site = 'default'
            and c.compCd = 'default'
            and c.codeClassId = :codeClassId
            and c.flagActive = true 
    """)
    fun getInitialCodes(codeClassId:String):List<Code?>
}

interface VendorRep : JpaRepository<Vendor,Long>{

    @Query(
        value = """
            select v
            from Vendor v
            where v.site = :site
            and   v.compCd = :compCd
            and   (v.vendorId like concat ('%',:vendorId,'%'))
            and   (v.vendorName like concat ('%',:vendorName,'%'))
            and   (v.ceoName like concat ('%',:ceoName,'%'))
            and   (v.businessType like concat ('%',:businessType,'%'))
            and   (:flagActive is null or v.flagActive = :flagActive)
        """
    )
    fun getVendorList(
        site:String,
        compCd:String,
        vendorId:String,
        vendorName:String,
        ceoName:String,
        businessType:String,
        flagActive:Boolean?
    ):List<Vendor?>

    @Query(
        value = """
            select v
            from Vendor v
            where v.site = :site
            and   v.compCd = :compCd
            and   v.vendorId IN (:vendorIds)
        """
    )
    fun getVendorListByIds(
        site:String,
        compCd:String,
        vendorIds:List<String?>
    ):List<Vendor?>


    @Transactional
    @Modifying
    @Query("""
        delete 
        from Vendor v 
        where v.site = :site
        and   v.compCd = :compCd
        and   v.vendorId = :vendorId
        """
    )
    fun deleteByVendorId(
        site:String,
        compCd:String,
        vendorId: String
    ): Int

}

interface LineRep : JpaRepository<Line,Long>{

    @Query(
        value = """
            select new kr.co.imoscloud.service.standardInfo.LineResponseModel(
                f.factoryId,
                f.factoryName,
                f.factoryCode,
                l.lineId,
                l.lineName,
                l.lineDesc,
                case when l.flagActive = true then 'Y' else 'N' end,
                l.createUser,
                l.createDate,
                l.updateUser,
                l.updateDate
            )
            from  Line l
            join  Factory  f
            on  l.site = f.site
            and l.compCd = f.compCd
            and l.factoryId = f.factoryId
            where l.site = :site
            and   l.compCd = :compCd
            and   (l.factoryId like concat ('%',:factoryId,'%'))
            and   (f.factoryName like concat ('%',:factoryName,'%'))
            and   (f.factoryCode like concat ('%',:factoryCode,'%'))
            and   (l.lineId like concat ('%',:lineId,'%'))
            and   (l.lineName like concat ('%',:lineName,'%'))
            and   (:flagActive is null or  l.flagActive = :flagActive)
        """
    )
    fun getLines(
        site:String,
        compCd:String,
        factoryId:String,
        factoryName:String,
        factoryCode:String,
        lineId:String,
        lineName:String,
        flagActive:Boolean?
    ):List<LineResponseModel?>

    @Query(
        value = """
            select l
            from Line l
            where l.site = :site
            and   l.compCd = :compCd
            and   l.lineId IN (:lineIds)
        """
    )
    fun getLineListByIds(
        site:String,
        compCd:String,
        lineIds:List<String?>
    ):List<Line?>

    @Transactional
    @Modifying
    @Query("""
        delete 
        from Line l 
        where l.site = :site
        and   l.compCd = :compCd
        and   l.lineId = :lineId
        """
    )
    fun deleteByLineId(
        site:String,
        compCd:String,
        lineId: String
    ): Int


    @Query(
        value = """
            select l
            from Line l
            where l.site = :site
            and   l.compCd = :compCd
            and   l.flagActive = true
        """
    )
    fun getLineOptions(
        site:String,
        compCd:String
    ):List<Line?>

}

interface WarehouseRep : JpaRepository<Warehouse, Long>{
    @Query(
        value = """
            select new kr.co.imoscloud.service.standardInfo.WarehouseResponse(
                w.factoryId,
                f.factoryName,
                w.warehouseId,
                w.warehouseName,
                w.warehouseType,
                case when w.flagActive = true then 'Y' else 'N' end,
                w.createUser,
                w.createDate,
                w.updateUser,
                w.updateDate
            )
            from  Warehouse w
            join  Factory  f
            on  w.site = f.site
            and w.compCd = f.compCd
            and w.factoryId = f.factoryId
            where w.site = :site
            and   w.compCd = :compCd
            and   (w.factoryId like concat ('%',:factoryId,'%'))
            and   (f.factoryName like concat ('%',:factoryName,'%'))
            and   (w.warehouseId like concat ('%',:warehouseId,'%'))
            and   (w.warehouseName like concat ('%',:warehouseName,'%'))
            and   (:flagActive is null or  w.flagActive = :flagActive)
        """
    )
    fun getWarehouses(
        site:String,
        compCd:String,
        factoryId:String,
        factoryName:String,
        warehouseId:String,
        warehouseName:String,
        flagActive:Boolean?
    ):List<WarehouseResponse?>

    @Query(
        value = """
            select w
            from Warehouse w
            where w.site = :site
            and   w.compCd = :compCd
            and   w.warehouseId IN (:warehouseIds)
        """
    )
    fun getWarehouseListByIds(
        site:String,
        compCd:String,
        warehouseIds:List<String?>
    ):List<Warehouse?>

    @Transactional
    @Modifying
    @Query("""
        delete 
        from Warehouse w 
        where w.site = :site
        and   w.compCd = :compCd
        and   w.warehouseId = :warehouseId
        """
    )
    fun deleteByWarehouseId(
        site:String,
        compCd:String,
        warehouseId: String
    ): Int
}

interface EquipmentRep:JpaRepository<Equipment,Long>{
    @Query(
        value = """
            select new kr.co.imoscloud.service.standardInfo.EquipmentResponseModel(
                e.factoryId,
                f.factoryName,
                e.lineId,
                l.lineName,
                e.equipmentId,
                e.equipmentBuyDate,
                e.equipmentBuyVendor,
                e.equipmentSn,
                e.equipmentType,
                e.equipmentName,
                e.equipmentStatus,
                case when e.flagActive = true then 'Y' else 'N' end,
                e.createUser,
                e.createDate,
                e.updateUser,
                e.updateDate
            )
            from  Equipment e
            join  Factory  f
            on  e.site = f.site
            and e.compCd = f.compCd
            and e.factoryId = f.factoryId
            join Line l
            on e.site = l.site
            and e.compCd = l.compCd
            and e.factoryId = l.factoryId
            and e.lineId = l.lineId
            where e.site = :site
            and   e.compCd = :compCd
            and   (e.factoryId like concat ('%',:factoryId,'%'))
            and   (f.factoryName like concat ('%',:factoryName,'%'))
            and   (e.lineId like concat ('%',:lineId,'%'))
            and   (l.lineName like concat ('%',:lineName,'%'))
            and   (e.equipmentId like concat ('%',:equipmentId,'%'))
            and   (e.equipmentName like concat ('%',:equipmentName,'%'))
            and   (e.equipmentSn like concat ('%',:equipmentSn,'%'))
            and   (e.equipmentType like concat ('%',:equipmentType,'%'))
            and   (:flagActive is null or  e.flagActive = :flagActive)
        """
    )
    fun getEquipments(
        site:String,
        compCd:String,
        factoryId:String,
        factoryName:String,
        lineId:String,
        lineName:String,
        equipmentId:String,
        equipmentName:String,
        equipmentSn:String,
        equipmentType:String,
        flagActive:Boolean?
    ):List<EquipmentResponseModel?>

    @Query(
        value = """
            select e
            from Equipment e
            where e.site = :site
            and   e.compCd = :compCd
            and   e.equipmentId IN (:equipmentIds)
        """
    )
    fun getEquipmentListByIds(
        site:String,
        compCd:String,
        equipmentIds:List<String?>
    ):List<Equipment?>

    @Transactional
    @Modifying
    @Query("""
        delete 
        from Equipment e 
        where e.site = :site
        and   e.compCd = :compCd
        and   e.equipmentId = :equipmentId
        """
    )
    fun deleteByEquipmentId(
        site:String,
        compCd:String,
        equipmentId: String
    ): Int

}