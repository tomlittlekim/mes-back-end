package kr.co.imoscloud.service

import jakarta.transaction.Transactional
import kr.co.imoscloud.core.Core
import kr.co.imoscloud.entity.inventory.InventoryIn
import kr.co.imoscloud.entity.inventory.InventoryInManagement
import kr.co.imoscloud.entity.inventory.InventoryOut
import kr.co.imoscloud.entity.inventory.InventoryOutManagement
import kr.co.imoscloud.fetcher.inventory.*
import kr.co.imoscloud.repository.InventoryInManagementRep
import kr.co.imoscloud.repository.InventoryInRep
import kr.co.imoscloud.repository.InventoryOutManagementRep
import kr.co.imoscloud.repository.InventoryOutRep
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipalOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class InventoryService (
    val inventoryInManagementRep: InventoryInManagementRep,
    val inventoryInRep: InventoryInRep,
    val inventoryOutManagementRep: InventoryOutManagementRep,
    val inventoryOutRep: InventoryOutRep,
    val core: Core
){

    /*
    * TODO:안 - 공통
    * inType -> 드롭다운 방식으로 수정*/

    fun getInventoryInManagementListWithFactoryAndWarehouse(filter: InventoryInManagementFilter): List<InventoryInManagementResponseModel?> {

        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        return inventoryInManagementRep.findInventoryInManagementWithMaterialInfo(
            inManagementId = filter.inManagementId ?: "",
            inType = filter.inType ?: "",
            factoryName = filter.factoryName ?: "",
            warehouseName = filter.warehouseName ?: "",
            createUser = filter.createUser ?: "",
            hasInvoice = filter.hasInvoice,
            startDate = filter.startDate ?: "",
            endDate = filter.endDate ?: "",
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            flagActive = true,
        )
    }

    fun getInventoryInListWithMaterial(filter: InventoryInFilter): List<InventoryInResponseModel?> {

        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        return inventoryInRep.findInventoryInWithMaterial(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            inManagementId = filter.inManagementId ?: throw IllegalArgumentException("입고관리번호는 필수입니다.")
        )
    }

    @Transactional
    fun saveInventoryIn(
        createdRows: List<InventoryInSaveInput?>,
        updatedRows:List<InventoryInUpdateInput?>
    ){
        createDetailedInventory(createdRows)
        updateDetailedInventory(updatedRows)
    }

    @Transactional
    fun saveInventoryInManagement(
        createdRows: List<InventoryInManagementSaveInput?>,
    ){

        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        val now = LocalDate.now()

        // 1. 마지막 inManagementId 가져오기
        val lastEntity = inventoryInManagementRep.findTopByOrderByInManagementIdDesc()
        val lastIdNumber = lastEntity?.inManagementId
            ?.removePrefix("IN")
            ?.toLongOrNull() ?: 0L

        // 2. 생성할 엔티티 리스트 만들기
        val inventoryList = createdRows.mapIndexedNotNull { index, input ->
            input?.let {
                val newIdNumber = lastIdNumber + 1
                val newInManagementId = "IN$newIdNumber"

                InventoryInManagement().apply {
                    site = currentUser.getSite()
                    compCd = currentUser.compCd
                    factoryId = it.factoryId
                    warehouseId = it.warehouseId
                    totalPrice = it.totalPrice?.toIntOrNull()
                    hasInvoice = it.hasInvoice
                    remarks = null
                    flagActive = true
                    createUser = currentUser.loginId
                    createDate = now
                    updateUser = currentUser.loginId
                    updateDate = now
                    inManagementId = newInManagementId
                    inType = it.inType
                }
            }
        }

        inventoryInManagementRep.saveAll(inventoryList)
    }

    @Transactional
    fun deleteInventoryInManagement(param: InventoryInManagementDeleteInput){
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        inventoryInManagementRep.deleteByInManagementIdAndSiteAndCompCd(
            param.inManagementId,
            site = currentUser.getSite(),
            compCd = currentUser.compCd,)
        inventoryInRep.deleteByInManagementIdAndSiteAndCompCd(
            param.inManagementId,
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
        )
    }

    @Transactional
    fun deleteInventoryIn(param: InventoryInDeleteInput){
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        inventoryInRep.deleteByInInventoryIdAndSiteAndCompCd(
            param.inInventoryId,
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
        )
    }

    fun createDetailedInventory(createdRows: List<InventoryInSaveInput?>){
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        // 1. 마지막 입고 번호 조회
        val lastInManagementId = inventoryInRep.findTopByOrderByInManagementIdDesc()?.inManagementId

        // 2. 다음 입고 번호 생성
        val nextInManagementBase = if (lastInManagementId == null) {
            1
        } else {
            lastInManagementId.removePrefix("IN").toLongOrNull()?.plus(1) ?: 1
        }

        // 3. inventoryInList 생성
        val now = LocalDate.now()
        val inventoryInList = createdRows.mapIndexed { index, it ->
            InventoryIn().apply {
                site = currentUser.getSite()
                compCd = currentUser.compCd
                inManagementId = it?.inManagementId ?: "IN${nextInManagementBase + index}"
                systemMaterialId = it?.systemMaterialId
                inType = it?.inType
                qty = it?.qty?.toInt()
                unitPrice = it?.unitPrice?.toInt()
                unitVat = it?.unitVat?.toInt()
                totalPrice = it?.totalPrice?.toInt()
                flagActive = true
                createUser = currentUser.loginId
                createDate = now
                updateUser = currentUser.loginId
                updateDate = now
                inInventoryId = UUID.randomUUID().toString()
            }
        }

        inventoryInRep.saveAll(inventoryInList)
    }

    fun updateDetailedInventory(updatedRows: List<InventoryInUpdateInput?>){

        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        val detailedInInventoryListIds = updatedRows.map {
            it?.inInventoryId
        }

        val factoryList = inventoryInRep.getDetailedInventoryInListByIds(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            inInventoryId = detailedInInventoryListIds
        )

        val updateList = factoryList.associateBy { it?.inInventoryId }

        updatedRows.forEach{ x ->
            val inInventoryId = x?.inInventoryId
            val inInventory = updateList[inInventoryId]

            inInventory?.let{
                it.qty = x?.qty?.toInt()
                it.unitPrice = x?.unitPrice?.toInt()
                it.unitVat = x?.unitVat?.toInt()
                it.totalPrice = x?.totalPrice?.toInt()
            }
        }

        inventoryInRep.saveAll(factoryList)
    }

    // 출고관리 기능
    fun getInventoryOutManagementListWithFactoryAndWarehouse(filter: InventoryOutManagementFilter): List<InventoryOutManagementResponseModel?> {

        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        return inventoryOutManagementRep.findInventoryOutManagementWithMaterialInfo(
            outManagementId = filter.outManagementId ?: "",
            outType = filter.outType ?: "",
            factoryName = filter.factoryName ?: "",
            warehouseName = filter.warehouseName ?: "",
            createUser = filter.createUser ?: "",
            startDate = filter.startDate ?: "",
            endDate = filter.endDate ?: "",
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            flagActive = true,
        )
    }

    fun getInventoryOutListWithMaterial(filter: InventoryOutFilter): List<InventoryOutResponseModel?> {

        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        return inventoryOutRep.findInventoryOutWithMaterial(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            outManagementId = filter.outManagementId ?: throw IllegalArgumentException("출고관리번호는 필수입니다.")
        )
    }
    
    @Transactional
    fun saveInventoryOut(
        createdRows: List<InventoryOutSaveInput?>,
        updatedRows:List<InventoryOutUpdateInput?>
    ){
        createDetailedOutInventory(createdRows)
        updateDetailedOutInventory(updatedRows)
    }

    @Transactional
    fun saveInventoryOutManagement(
        createdRows: List<InventoryOutManagementSaveInput?>,
    ){

        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        val now = LocalDate.now()

        // 1. 마지막 outManagementId 가져오기
        val lastEntity = inventoryOutManagementRep.findTopByOrderByOutManagementIdDesc()
        val lastIdNumber = lastEntity?.outManagementId
            ?.removePrefix("OUT")
            ?.toLongOrNull() ?: 0L

        // 2. 생성할 엔티티 리스트 만들기
        val inventoryList = createdRows.mapIndexedNotNull { index, input ->
            input?.let {
                val newIdNumber = lastIdNumber + 1
                val newOutManagementId = "OUT$newIdNumber"

                InventoryOutManagement().apply {
                    site = currentUser.getSite()
                    compCd = currentUser.compCd
                    factoryId = it.factoryId
                    warehouseId = it.warehouseId
                    totalPrice = it.totalPrice?.toIntOrNull()
                    remarks = null
                    flagActive = true
                    createUser = currentUser.loginId
                    createDate = now
                    updateUser = currentUser.loginId
                    updateDate = now
                    outManagementId = newOutManagementId
                    outType = it.outType
                }
            }
        }

        inventoryOutManagementRep.saveAll(inventoryList)
    }

    @Transactional
    fun deleteInventoryOutManagement(param: InventoryOutManagementDeleteInput){
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        inventoryOutManagementRep.deleteByOutManagementIdAndSiteAndCompCd(
            param.outManagementId,
            site = currentUser.getSite(),
            compCd = currentUser.compCd,)
        inventoryOutRep.deleteByOutManagementIdAndSiteAndCompCd(
            param.outManagementId,
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
        )
    }

    @Transactional
    fun deleteInventoryOut(param: InventoryOutDeleteInput){
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        inventoryOutRep.deleteByOutInventoryIdAndSiteAndCompCd(
            param.outInventoryId,
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
        )
    }

    fun createDetailedOutInventory(createdRows: List<InventoryOutSaveInput?>){
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        // 1. 마지막 출고 번호 조회
        val lastOutManagementId = inventoryOutRep.findTopByOrderByOutManagementIdDesc()?.outManagementId

        // 2. 다음 출고 번호 생성
        val nextOutManagementBase = if (lastOutManagementId == null) {
            1
        } else {
            lastOutManagementId.removePrefix("OUT").toLongOrNull()?.plus(1) ?: 1
        }

        // 3. inventoryOutList 생성
        val now = LocalDate.now()
        val inventoryOutList = createdRows.mapIndexed { index, it ->
            InventoryOut().apply {
                site = currentUser.getSite()
                compCd = currentUser.compCd
                outManagementId = it?.outManagementId ?: "OUT${nextOutManagementBase + index}"
                systemMaterialId = it?.systemMaterialId
                outType = it?.outType
                qty = it?.qty?.toInt()
                unitPrice = it?.unitPrice?.toInt()
                unitVat = it?.unitVat?.toInt()
                totalPrice = it?.totalPrice?.toInt()
                flagActive = true
                createUser = currentUser.loginId
                createDate = now
                updateUser = currentUser.loginId
                updateDate = now
                outInventoryId = UUID.randomUUID().toString()
            }
        }

        inventoryOutRep.saveAll(inventoryOutList)
    }

    fun updateDetailedOutInventory(updatedRows: List<InventoryOutUpdateInput?>){

        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        val detailedOutInventoryListIds = updatedRows.map {
            it?.outInventoryId
        }

        val factoryList = inventoryOutRep.getDetailedInventoryOutListByIds(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            outInventoryId = detailedOutInventoryListIds
        )

        val updateList = factoryList.associateBy { it?.outInventoryId }

        updatedRows.forEach{ x ->
            val outInventoryId = x?.outInventoryId
            val outInventory = updateList[outInventoryId]

            outInventory?.let{
                it.qty = x?.qty?.toInt()
                it.unitPrice = x?.unitPrice?.toInt()
                it.unitVat = x?.unitVat?.toInt()
                it.totalPrice = x?.totalPrice?.toInt()
            }
        }

        inventoryOutRep.saveAll(factoryList)
    }
}

data class InventoryInManagementResponseModel(
    val inManagementId: String,
    val inType: String,
    val factoryName: String?,
    val warehouseName: String?,
    val materialInfo: String?,
    val totalPrice: Int?,
    val hasInvoice: String?,
    val userName: String?,
    val createDate: String?
)

data class InventoryInResponseModel(
    val inManagementId: String? = null,
    val inInventoryId: String? = null,
    val supplierName: String? = null,
    val manufacturerName: String? = null,
    val userMaterialId: String? = null,
    val materialName: String? = null,
    val materialCategory: String? = null,
    val materialStandard: String? = null,
    val qty: String? = null,
    val unitPrice: String? = null,
    val unitVat: String? = null,
    val totalPrice: String? = null,
    val createUser: String? = null,
    val createDate: String? = null,
    val updateUser: String? = null,
    val updateDate: String? = null
)

data class InventoryOutManagementResponseModel(
    val outManagementId: String,
    val outType: String,
    val factoryName: String?,
    val warehouseName: String?,
    val materialInfo: String?,
    val totalPrice: Int?,
    val userName: String?,
    val createDate: String?
)

data class InventoryOutResponseModel(
    val outManagementId: String? = null,
    val outInventoryId: String? = null,
    val supplierName: String? = null,
    val manufacturerName: String? = null,
    val userMaterialId: String? = null,
    val materialName: String? = null,
    val materialCategory: String? = null,
    val materialStandard: String? = null,
    val qty: String? = null,
    val unitPrice: String? = null,
    val unitVat: String? = null,
    val totalPrice: String? = null,
    val createUser: String? = null,
    val createDate: String? = null,
    val updateUser: String? = null,
    val updateDate: String? = null
)