package kr.co.imoscloud.repository

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.standardInfo.*
import kr.co.imoscloud.service.standardInfo.LineResponseModel
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
    fun deleteByVendorId(
        site:String,
        compCd:String,
        lineId: String
    ): Int

}