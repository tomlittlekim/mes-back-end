package kr.co.imoscloud.repository

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.material.MaterialMaster
import kr.co.imoscloud.entity.standardInfo.*
import kr.co.imoscloud.service.standardInfo.EquipmentResponseModel
import kr.co.imoscloud.service.standardInfo.LineResponseModel
import kr.co.imoscloud.service.standardInfo.WarehouseResponse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

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
            and   f.flagActive = true
        """
    )
    fun getFactoryList(
        site:String,
        compCd:String,
        factoryId:String,
        factoryName:String,
        factoryCode:String,
//        flagActive:Boolean?
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
        update Factory f
        set f.flagActive = false,
        f.updateUser = :updateUser,
        f.updateDate = :updateDate
        where f.factoryId = :factoryId
        and   f.site = :site
        and   f.compCd = :compCd
        """
    )
    fun deleteByFactoryId(
        site:String,
        compCd:String,
        factoryId: String,
        updateUser: String,
        updateDate: LocalDateTime = LocalDateTime.now()
    ): Int

    //단순 조회용 메서드 추가
    fun findByFactoryId(
        factoryId: String?
    ): Factory?
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
            and   cc.flagActive = true
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
            and   c.flagActive = true
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
        update Code c 
        set c.flagActive = false,
            c.updateUser = :updateUser,
            c.updateDate = :updateDate
        where c.site = :site
        and   c.compCd = :compCd
        and   c.codeId = :codeId
        """
    )
    fun deleteByCodeId(
        site:String,
        compCd:String,
        codeId: String,
        updateUser: String,
        updateDate: LocalDateTime = LocalDateTime.now()
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
        from Code c
        where c.site = 'default'
            and c.compCd = 'default'
            and c.codeClassId = :codeClassId
            and c.flagActive = true 
        order by c.sortOrder
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
            and   v.flagActive = true
        """
    )
    fun getVendorList(
        site:String,
        compCd:String,
        vendorId:String,
        vendorName:String,
        ceoName:String,
        businessType:String
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
        update Vendor v
        set v.flagActive = false,
            v.updateUser = :updateUser,
            v.updateDate = :updateDate
        where v.site = :site
        and   v.compCd = :compCd
        and   v.vendorId = :vendorId
        """
    )
    fun deleteByVendorId(
        site:String,
        compCd:String,
        vendorId: String,
        updateUser: String,
        updateDate: LocalDateTime = LocalDateTime.now()
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
            and   l.flagActive = true
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
//        flagActive:Boolean?
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
        update Line l
        set   l.flagActive = false,
              l.updateDate = :updateDate,
              l.updateUser = :updateUser
        where l.site = :site
        and   l.compCd = :compCd
        and   l.lineId = :lineId
        """
    )
    fun deleteByLineId(
        site:String,
        compCd:String,
        lineId: String,
        updateDate: LocalDateTime = LocalDateTime.now(),
        updateUser: String
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
            and   w.flagActive = true
        """
    )
    fun getWarehouses(
        site:String,
        compCd:String,
        factoryId:String,
        factoryName:String,
        warehouseId:String,
        warehouseName:String
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
        update Warehouse w
        set w.flagActive = false,
            w.updateUser = :updateUser,
            w.updateDate = :updateDate
        where w.site = :site
        and   w.compCd = :compCd
        and   w.warehouseId = :warehouseId
        """
    )
    fun deleteByWarehouseId(
        site:String,
        compCd:String,
        warehouseId: String,
        updateUser: String,
        updateDate: LocalDateTime = LocalDateTime.now(),
    ): Int

    //단순 조회용 메서드 추가
    fun findByWarehouseId(
        warehouseId: String?
    ): Warehouse?
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
            and   e.flagActive = true
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
        equipmentType:String
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
        update Equipment e
        set e.flagActive = false,
            e.updateUser = :updateUser,
            e.updateDate = :updateDate
        where e.site = :site
        and   e.compCd = :compCd
        and   e.equipmentId = :equipmentId
        """
    )
    fun deleteByEquipmentId(
        site:String,
        compCd:String,
        equipmentId: String,
        updateUser: String,
        updateDate:LocalDateTime = LocalDateTime.now()
    ): Int

}