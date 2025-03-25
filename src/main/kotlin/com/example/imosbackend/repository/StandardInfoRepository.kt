package com.example.imosbackend.repository

import com.example.imosbackend.entity.StandardInfo.Factory
import jakarta.transaction.Transactional
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