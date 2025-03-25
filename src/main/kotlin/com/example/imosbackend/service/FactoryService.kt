package com.example.imosbackend.service

import com.example.imosbackend.entity.StandardInfo.Factory
import com.example.imosbackend.entity.StandardInfo.FactoryFilter
import com.example.imosbackend.entity.StandardInfo.FactoryInput
import com.example.imosbackend.repository.FactoryRep
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class FactoryService(
    val factoryRep: FactoryRep
) {

    fun getFactories(filter: FactoryFilter): List<FactoryResponseModel?> {
        val factoryList = factoryRep.getFactoryList(
            site = "imos",
            compCd = "eightPin",
            factoryId = filter.factoryId,
            factoryName = filter.factoryName,
            factoryCode = filter.factoryCode,
            flagActive = filter.flagActive,
        )

        val result  = factoryList.map {
            FactoryResponseModel(
                it?.factoryId,
                it?.factoryName,
                it?.factoryCode,
                it?.address,
                if (it?.flagActive == true) "Y" else "N",
                it?.telNo,
                it?.officerName,
                it?.createUser,
                it?.createDate.toString(),
                it?.updateUser,
                it?.updateDate.toString()
            )
        }

        return result
    }

    fun createFactory(factoryInput: List<FactoryInput>){
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val factoryList = factoryInput.map {
            Factory(
                factoryId = "FAC" + LocalDateTime.now().format(formatter),
                site = "imos",
                compCd = "eightPin",
                factoryName = it.factoryName,
                factoryCode = it.factoryCode,
                address = it.address,
                telNo = it.telNo,
                officerName = it.officerName,
                flagActive = it.flagActive.equals("Y" ),
                createUser = "syh"
            )
        }

        factoryRep.saveAll(factoryList)
    }

    fun deleteFactory(factoryId:String): Boolean {
        return factoryRep.deleteByFactoryId(
            site = "imos",
            compCd = "eightPin",
            factoryId = factoryId
        ) > 0
    }

}

data class FactoryResponseModel(
    val factoryId: String? = null,
    val factoryName: String? = null,
    val factoryCode: String? = null,
    val address:String? = null,
    val flagActive:String? = null,
    val telNo: String? = null,
    val officerName: String? = null,
    val createUser: String? = null,
    val createDate: String? = null,
    val updateUser: String? = null,
    val updateDate: String? = null,
)
