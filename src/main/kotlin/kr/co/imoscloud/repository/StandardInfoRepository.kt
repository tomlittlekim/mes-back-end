package kr.co.imoscloud.repository

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.standardInfo.CodeClass
import kr.co.imoscloud.entity.standardInfo.Factory
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
}