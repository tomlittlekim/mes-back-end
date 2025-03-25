package com.example.imosbackend.repository

import com.example.imosbackend.entity.Factory
import com.example.imosbackend.service.FactoryResponseModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface FactoryRep: JpaRepository<Factory,Long>{
    @Query(
        value = """
            select new com.example.imosbackend.service.FactoryResponseModel(
                f.factoryId,
                f.factoryName,
                f.factoryCode,
                f.address,
                f.flagActive,
                f.telNo,
                f.officerName
            ) 
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
    ):List<FactoryResponseModel?>
}