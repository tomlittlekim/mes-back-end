package com.example.imosbackend.service

import com.example.imosbackend.entity.*
import com.example.imosbackend.repository.FactoryRep
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class FactoryService(
    val factoryRep: FactoryRep
) {
    //신용희 IQ 생각보다 높지 않음
    fun getFactories(filter:FactoryFilter): List<FactoryResponseModel?> {
        val result = factoryRep.getFactoryList(
            site = "imos",
            compCd = "eightPin",
            factoryId = filter.factoryId,
            factoryName = filter.factoryName,
            factoryCode = filter.factoryCode,
            flagActive = filter.flagActive
        )

        return result
    }
}

data class FactoryResponseModel(
    val factoryId: String? = null,
    val factoryName: String? = null,
    val factoryCode: String? = null,
    val address:String? = null,
    val flagActive:Boolean = false,
    val telNo: String? = null,
    val officerName: String? = null,
)