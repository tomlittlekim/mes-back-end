package kr.co.imoscloud.service.standardInfo

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.standardInfo.Factory
import kr.co.imoscloud.fetcher.standardInfo.FactoryFilter
import kr.co.imoscloud.fetcher.standardInfo.FactoryInput
import kr.co.imoscloud.fetcher.standardInfo.FactoryUpdate
import kr.co.imoscloud.repository.FactoryRep
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

//TODO site, compCd  security 에 저장된 정보로 로직 변경

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
            flagActive = filter.flagActive?.let { it == "Y" }
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

    @Transactional
    fun saveFactory(createdRows: List<FactoryInput?>, updatedRows:List<FactoryUpdate?>){
        //TODO 저장 ,수정시 공통 으로 작성자 ,작성일 ,수정자 ,수정일 변경 저장이 필요함
        createFactory(createdRows)
        updateFactory(updatedRows)
    }

    fun createFactory(createdRows: List<FactoryInput?>){
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val factoryList = createdRows.map {
            Factory(
                factoryId = "FAC" + LocalDateTime.now().format(formatter),
                site = "imos",
                compCd = "eightPin",
                factoryName = it?.factoryName,
                factoryCode = it?.factoryCode,
                address = it?.address,
                telNo = it?.telNo,
                officerName = it?.officerName,
                flagActive = it?.flagActive.equals("Y" ),
                createUser = "syh"
            )
        }

        factoryRep.saveAll(factoryList)
    }

    fun updateFactory(updatedRows: List<FactoryUpdate?>){
        val factoryListId = updatedRows.map {
            it?.factoryId
        }

        val factoryList = factoryRep.getFactoryListByIds(
            site = "imos",
            compCd = "eightPin",
            factoryIds = factoryListId
        )

        val updateList = factoryList.associateBy { it?.factoryId }

        updatedRows.forEach{ x ->
            val factoryId = x?.factoryId
            val factory = updateList[factoryId]

            factory?.let{
                it.factoryName = x?.factoryName
                it.factoryCode = x?.factoryCode
                it.address = x?.address
                it.telNo = x?.telNo
                it.officerName = x?.officerName
                it.flagActive = x?.flagActive.equals("Y" )
            }
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
