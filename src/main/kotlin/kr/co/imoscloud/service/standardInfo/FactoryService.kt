package kr.co.imoscloud.service.standardInfo

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.standardInfo.Factory
import kr.co.imoscloud.fetcher.standardInfo.FactoryFilter
import kr.co.imoscloud.fetcher.standardInfo.FactoryInput
import kr.co.imoscloud.fetcher.standardInfo.FactoryUpdate
import kr.co.imoscloud.repository.FactoryRep
import kr.co.imoscloud.security.UserPrincipal
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class FactoryService(
    val factoryRep: FactoryRep
) {

    fun getFactories(filter: FactoryFilter): List<FactoryResponseModel?> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        val factoryList = factoryRep.getFactoryList(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            factoryId = filter.factoryId,
            factoryName = filter.factoryName,
            factoryCode = filter.factoryCode
        )

        val result  = entityToResponse(factoryList)

        return result
    }

    @Transactional
    fun saveFactory(createdRows: List<FactoryInput?>, updatedRows:List<FactoryUpdate?>){
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        createdRows.filterNotNull().takeIf { it.isNotEmpty() }?.let {createFactory(it, userPrincipal)}
        updatedRows.filterNotNull().takeIf { it.isNotEmpty() }?.let {updateFactory(it, userPrincipal)}
    }

    fun createFactory(createdRows: List<FactoryInput?>, userPrincipal: UserPrincipal){
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val factoryList = createdRows.map {
            Factory(
                factoryId = "FAC" + LocalDateTime.now().format(formatter) +
                        System.nanoTime().toString().takeLast(3),
                site = userPrincipal.getSite(),
                compCd = userPrincipal.compCd,
                factoryName = it?.factoryName,
                factoryCode = it?.factoryCode,
                address = it?.address,
                telNo = it?.telNo,
                remark = it?.remark,
            ).apply{
                createCommonCol(userPrincipal)
            }
        }

        factoryRep.saveAll(factoryList)
    }

    fun updateFactory(updatedRows: List<FactoryUpdate?>,userPrincipal: UserPrincipal){
        val factoryListIds = updatedRows.map {
            it?.factoryId
        }

        val factoryList = factoryRep.getFactoryListByIds(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            factoryIds = factoryListIds
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
                it.remark = x?.remark
                it.updateCommonCol(userPrincipal)
            }
        }

        factoryRep.saveAll(factoryList)
    }

    fun deleteFactory(factoryIds:List<String>): Boolean {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        return factoryRep.deleteByFactoryId(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            updateUser = userPrincipal.loginId,
            factoryIds = factoryIds
        ) > 0
    }

    fun getGridFactory(): List<FactoryResponseModel> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        val factoryList = factoryRep.getGridFactory(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd
        )

        return entityToResponse(factoryList)
    }

    private fun entityToResponse(factoryList:List<Factory?>): List<FactoryResponseModel> {
        return factoryList.map {
            FactoryResponseModel(
                it?.factoryId,
                it?.factoryName,
                it?.factoryCode,
                it?.address,
//                if (it?.flagActive == true) "Y" else "N",
                it?.telNo,
                it?.remark,
                it?.createUser,
                it?.createDate.toString().replace("T", " "),
                it?.updateUser,
                it?.updateDate.toString().replace("T", " "),
            )
        }
    }

}

data class FactoryResponseModel(
    val factoryId: String? = null,
    val factoryName: String? = null,
    val factoryCode: String? = null,
    val address:String? = null,
//    val flagActive:String? = null,
    val telNo: String? = null,
    val remark: String? = null,
    val createUser: String? = null,
    val createDate: String? = null,
    val updateUser: String? = null,
    val updateDate: String? = null,
)
