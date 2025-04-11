package kr.co.imoscloud.service.inventory

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.inventory.*
import kr.co.imoscloud.entity.material.MaterialMaster
import kr.co.imoscloud.entity.standardInfo.Factory
import kr.co.imoscloud.entity.standardInfo.Warehouse
import kr.co.imoscloud.fetcher.inventory.*
import kr.co.imoscloud.repository.FactoryRep
import kr.co.imoscloud.repository.WarehouseRep
import kr.co.imoscloud.repository.inventory.*
import kr.co.imoscloud.repository.material.MaterialRepository
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipalOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class InventoryService(
    private val inventoryInManagementRep: InventoryInManagementRep,
    private val inventoryInRep: InventoryInRep,
    private val inventoryOutManagementRep: InventoryOutManagementRep,
    private val inventoryOutRep: InventoryOutRep,
    private val inventoryStatusRep: InventoryStatusRep,
    private val inventoryHistoryRep: InventoryHistoryRep,
    private val materialMasterRep: MaterialRepository,
    private val warehouseRep: WarehouseRep,
    private val factoryRep: FactoryRep,
){

    fun getInventoryInManagementListWithFactoryAndWarehouse(filter: InventoryInManagementFilter): List<InventoryInManagementResponseModel?> {

        val currentUser = getCurrentUserPrincipal()
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

        val now = LocalDateTime.now()

        // 2. 생성할 엔티티 리스트 만들기
        val inventoryList = createdRows.mapIndexedNotNull { index, input ->
            input?.let {
                // 각 항목마다 고유한 UUID 기반 ID 생성
                val randomId = UUID.randomUUID().toString().substring(0, 8)
                val newInManagementId = "IN$randomId"

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

        // 삭제할 입고 항목들 조회
        val inventoryInItems = inventoryInRep.findByInManagementIdAndSiteAndCompCd(
            param.inManagementId,
            currentUser.getSite(),
            currentUser.compCd
        )
        
        // 재고 조정을 위한 준비
        if (inventoryInItems.isNotEmpty()) {
            // 영향 받는 품목 ID 및 수량 정보 수집
            val materialQtyMap = inventoryInItems.filter { it.systemMaterialId != null }
                .groupBy { it.systemMaterialId }
                .mapValues { entry -> entry.value.sumOf { it.qty ?: 0 } }
                
            // 현재 재고 상태 조회
            val materialIds = materialQtyMap.keys.filterNotNull()
            if (materialIds.isNotEmpty()) {
                val currentInventoryStatus = inventoryStatusRep.findByCompCdAndSiteAndSystemMaterialIdIn(
                    currentUser.compCd,
                    currentUser.getSite(),
                    materialIds
                ).associateBy { it.systemMaterialId }
                
                // 재고 조정 및 로그 생성
                val statusesToUpdate = mutableListOf<InventoryStatus>()
                val logsToSave = mutableListOf<InventoryHistory>()
                
                materialQtyMap.forEach { (materialId, qty) ->
                    if (materialId != null) {
                        val status = currentInventoryStatus[materialId]
                        
                        if (status != null) {
                            // 기존 재고에서 삭제되는 입고 수량만큼 감소
                            var currentQty = status.qty ?: 0
                            status.qty = currentQty - qty
                            status.updateUser = currentUser.loginId
                            status.updateDate = LocalDateTime.now()
                            statusesToUpdate.add(status)

                            val materialInfo = getMaterialInfo(status.systemMaterialId)
                            val warehouseInfo = getWareHouseInfo(status.warehouseId)
                            val factoryInfo = getFactoryInfo(status.factoryId)

                            // 로그 추가
                            logsToSave.add(InventoryHistory().apply {
                                site = currentUser.getSite()
                                compCd = currentUser.compCd
                                warehouseName = warehouseInfo?.warehouseName
                                factoryName = factoryInfo?.factoryName
                                supplierName = materialInfo?.supplierName
                                manufacturerName = materialInfo?.manufacturerName
                                materialName = materialInfo?.materialName
                                unit = materialInfo?.unit
                                inOutType = "DELETE"
                                prevQty = currentQty
                                changeQty = -qty
                                currentQty -= qty
                                reason = "입고 삭제: ${param.inManagementId}"
                                createUser = currentUser.loginId
                                createDate = LocalDateTime.now()
                            })
                        }
                    }
                }

                // 상태 및 로그 저장
                if (statusesToUpdate.isNotEmpty()) {
                    inventoryStatusRep.saveAll(statusesToUpdate)
                }

                if (logsToSave.isNotEmpty()) {
                    inventoryHistoryRep.saveAll(logsToSave)
                }
            }
        }

        // 입고 관리 및 입고 항목 삭제
        inventoryInManagementRep.deleteByInManagementIdAndSiteAndCompCd(
            param.inManagementId,
            site = currentUser.getSite(),
            compCd = currentUser.compCd)
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

        // 삭제할 입고 항목 조회
        val inventoryIn = inventoryInRep.findByInInventoryIdAndSiteAndCompCd(
            param.inInventoryId,
            currentUser.getSite(),
            currentUser.compCd
        )

        // 재고 조정
        if (inventoryIn?.systemMaterialId != null) {
            var systemMaterialId = inventoryIn.systemMaterialId
            val qtyToRemove = inventoryIn.qty ?: 0

            // 현재 재고 상태 조회
            val inventoryStatus = systemMaterialId?.let {
                inventoryStatusRep.findByCompCdAndSiteAndSystemMaterialId(
                    currentUser.compCd,
                    currentUser.getSite(),
                    it
                )
            }

            if (inventoryStatus != null) {
                // 재고 감소
                var currentQty = inventoryStatus.qty ?: 0
                inventoryStatus.qty = currentQty - qtyToRemove
                inventoryStatus.updateUser = currentUser.loginId
                inventoryStatus.updateDate = LocalDateTime.now()
                inventoryStatusRep.save(inventoryStatus)

                // 로그 추가
                val log = InventoryHistory().apply {
                    site = currentUser.getSite()
                    compCd = currentUser.compCd
                    systemMaterialId = inventoryIn.systemMaterialId
                    prevQty = currentQty
                    changeQty = -qtyToRemove
                    currentQty = currentQty - qtyToRemove
                    inOutType = "DELETE"
                    reason = "개별 입고 삭제: ${inventoryIn.inInventoryId}"
                    createUser = currentUser.loginId
                    createDate = LocalDateTime.now()
                }
                inventoryHistoryRep.save(log)
            }
        }

        // 입고 항목 삭제
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
        val now = LocalDateTime.now()
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
        // 재고 현황 등록
        updateInventory(inventoryInList)
        // 재고 이력 등록
    }

    fun updateInventory(items: List<Any?>) {
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        // 시작 전에 한 번만 최대 seq 값 조회
        val currentSite = currentUser.getSite()
        val currentCompCd = currentUser.compCd

        //공장정보 및 창고정보 받아옴
        val inManagementInfo =
            items.filterIsInstance<InventoryIn>()
                .firstOrNull()
                ?.let { item ->
                    inventoryInManagementRep.findByCompCdAndSiteAndInManagementId(
                        item.compCd,
                        item.site,
                        item.inManagementId
                    )
                }

        val outManagementInfo =
            items.filterIsInstance<InventoryOut>()
                .firstOrNull()
                ?.let { item ->
                    inventoryOutManagementRep.findByOutManagementIdAndSiteAndCompCd(
                        item.outManagementId ?: "",
                        item.site ?: "",
                        item.compCd ?: ""
                    )
                }

        // 기존 재고 현황을 조회해서 맵으로 저장 (systemMaterialId가 키)
        val systemMaterialIds = items.mapNotNull { item ->
            when (item) {
                is InventoryIn -> item.systemMaterialId
                is InventoryOut -> item.systemMaterialId
                else -> null
            }
        }.distinct()

        // 필요한 systemMaterialId들에 대한 재고 현황을 한 번에 조회
        val existingInventoryList = if (systemMaterialIds.isNotEmpty()) {
            inventoryStatusRep.findByCompCdAndSiteAndSystemMaterialIdIn(
                currentCompCd,
                currentSite,
                systemMaterialIds
            )
        } else {
            emptyList()
        }

        // systemMaterialId를 키로 하는 맵으로 변환
        val existingInventoryMap = existingInventoryList.associateBy { it.systemMaterialId }

        // 업데이트할 재고 목록
        val inventoryStatusToUpdate = mutableListOf<InventoryStatus>()
        // 이력을 남길 로그 목록
        val inventoryLogsToSave = mutableListOf<InventoryHistory>()

        for (item in items) {
            when (item) {
                is InventoryIn -> {
                    if (item.systemMaterialId == null) continue

                    // 기존 재고가 있는지 확인
                    val existingStatus = existingInventoryMap[item.systemMaterialId]
                    val oldQty = existingStatus?.qty ?: 0
                    val newQty = item.qty ?: 0

                    var newStatus: InventoryStatus? = null

                    if (existingStatus != null) {
                        // 기존 재고가 있으면 수량 업데이트
                        existingStatus.qty = existingStatus.qty?.plus(newQty)
                        existingStatus.updateUser = item.updateUser
                        existingStatus.updateDate = item.updateDate
                        inventoryStatusToUpdate.add(existingStatus)
                    } else {
                        // 기존 재고가 없으면 새로 생성
                        newStatus = InventoryStatus().apply {
                            site = item.site
                            compCd = item.compCd
                            factoryId = inManagementInfo?.factoryId
                            warehouseId = inManagementInfo?.warehouseId
                            systemMaterialId = item.systemMaterialId
                            qty = item.qty
                            flagActive = true
                            createUser = item.createUser
                            createDate = item.createDate
                            updateUser = item.updateUser
                            updateDate = item.updateDate
                        }
                        inventoryStatusToUpdate.add(newStatus)
                    }

                    val materialInfo = getMaterialInfo(item.systemMaterialId)
                    val warehouseInfo = getWareHouseInfo(inManagementInfo?.warehouseId)
                    val factoryInfo = getFactoryInfo(inManagementInfo?.factoryId)

                    // 재고 로그 생성
                    inventoryLogsToSave.add(InventoryHistory().apply {
                        site = item.site
                        compCd = item.compCd
                        warehouseName = warehouseInfo?.warehouseName
                        factoryName = factoryInfo?.factoryName
                        supplierName = materialInfo?.supplierName
                        manufacturerName = materialInfo?.manufacturerName
                        materialName = materialInfo?.materialName
                        unit = materialInfo?.unit
                        prevQty = oldQty
                        changeQty = newQty
                        currentQty = oldQty + newQty
                        inOutType = "IN"
                        reason = "입고: ${item.inManagementId}"
                        createUser = currentUser.loginId
                        createDate = LocalDateTime.now()
                    })
                }

                is InventoryOut -> {
                    if (item.systemMaterialId == null) continue

                    // 기존 재고가 있는지 확인
                    val existingStatus = existingInventoryMap[item.systemMaterialId]
                    val oldQty = existingStatus?.qty ?: 0
                    val outQty = item.qty?.toInt() ?: 0
                    var newStatus: InventoryStatus? = null

                    if (existingStatus != null) {
                        // 기존 재고가 있으면 수량 감소
                        existingStatus.qty = existingStatus.qty?.minus(outQty)
                        existingStatus.updateUser = item.updateUser
                        existingStatus.updateDate = item.updateDate
                        inventoryStatusToUpdate.add(existingStatus)
                        
                        // 기존 재고에서 출고할 때 재고 로그 생성
                        val materialInfo = getMaterialInfo(existingStatus.systemMaterialId)
                        val warehouseInfo = getWareHouseInfo(existingStatus.warehouseId)
                        val factoryInfo = getFactoryInfo(existingStatus.factoryId)
                        
                        inventoryLogsToSave.add(InventoryHistory().apply {
                            site = item.site
                            compCd = item.compCd
                            warehouseName = warehouseInfo?.warehouseName
                            factoryName = factoryInfo?.factoryName
                            supplierName = materialInfo?.supplierName
                            manufacturerName = materialInfo?.manufacturerName
                            materialName = materialInfo?.materialName
                            unit = materialInfo?.unit
                            prevQty = oldQty
                            changeQty = -outQty
                            currentQty = oldQty - outQty
                            inOutType = "OUT"
                            reason = "출고: ${item.outManagementId}"
                            createUser = currentUser.loginId
                            createDate = LocalDateTime.now()
                        })
                    } else {
                        // 출고의 경우 기존 재고가 없으면 음수 재고로 생성
                        println("경고: ${item.systemMaterialId} 재료의 재고가 없는데 출고 시도됨")

                        newStatus = InventoryStatus().apply {
                            site = item.site
                            compCd = item.compCd
                            factoryId = outManagementInfo?.factoryId
                            warehouseId = outManagementInfo?.warehouseId
                            systemMaterialId = item.systemMaterialId
                            qty = -outQty  // 음수 값으로 설정
                            flagActive = true
                            createUser = item.createUser
                            createDate = item.createDate
                            updateUser = item.updateUser
                            updateDate = item.updateDate
                        }
                        inventoryStatusToUpdate.add(newStatus)
                        
                        // 새 재고 상태 생성시 재고 로그 추가
                        val materialInfo = getMaterialInfo(item.systemMaterialId)
                        val warehouseInfo = getWareHouseInfo(outManagementInfo?.warehouseId)
                        val factoryInfo = getFactoryInfo(outManagementInfo?.factoryId)
                        
                        inventoryLogsToSave.add(InventoryHistory().apply {
                            site = item.site
                            compCd = item.compCd
                            warehouseName = warehouseInfo?.warehouseName
                            factoryName = factoryInfo?.factoryName
                            supplierName = materialInfo?.supplierName
                            manufacturerName = materialInfo?.manufacturerName
                            materialName = materialInfo?.materialName
                            unit = materialInfo?.unit
                            prevQty = 0
                            changeQty = -outQty
                            currentQty = -outQty
                            inOutType = "OUT"
                            reason = "출고(재고없음): ${item.outManagementId}"
                            createUser = currentUser.loginId
                            createDate = LocalDateTime.now()
                        })
                    }
                }

                else -> {
                    println("Unknown type: $item")
                }
            }
        }

        // 한 번에 저장
        if (inventoryStatusToUpdate.isNotEmpty()) {
            inventoryStatusRep.saveAll(inventoryStatusToUpdate)
        }

        // 이력 로그 저장
        if (inventoryLogsToSave.isNotEmpty()) {
            inventoryHistoryRep.saveAll(inventoryLogsToSave)
        }
    }

    @Transactional
    fun updateDetailedInventory(updatedRows: List<InventoryInUpdateInput?>){
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        val detailedInInventoryListIds = updatedRows.map {
            it?.inInventoryId
        }

        // 기존 인벤토리 항목 조회
        val inventoryInList = inventoryInRep.getDetailedInventoryInListByIds(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            inInventoryId = detailedInInventoryListIds
        )

        val updateList = inventoryInList.associateBy { it?.inInventoryId }

        // 품목 변경을 추적하기 위한 맵
        val materialChanges = mutableMapOf<String?, Pair<String?, String?>>() // inInventoryId -> (oldMaterialId, newMaterialId)
        val qtyChanges = mutableMapOf<String?, Pair<Int?, Int?>>() // systemMaterialId -> (oldQty, newQty)

        // 변경 사항 추적
        updatedRows.forEach { x ->
            val inInventoryId = x?.inInventoryId
            val inInventory = updateList[inInventoryId]

            inInventory?.let {
                // 품목 변경 추적
                if (it.systemMaterialId != x?.systemMaterialId) {
                    materialChanges[inInventoryId] = Pair(it.systemMaterialId, x?.systemMaterialId)
                }

                // 수량 변경 추적
                if (it.qty != x?.qty?.toInt()) {
                    // 기존 품목에 대한 수량 변경
                    val oldMaterialId = it.systemMaterialId
                    if (oldMaterialId != null) {
                        val oldChange = qtyChanges[oldMaterialId] ?: Pair(0, 0)
                        qtyChanges[oldMaterialId] = Pair(oldChange.first?.plus(it.qty ?: 0), oldChange.second)
                    }

                    // 새 품목에 대한 수량 변경
                    val newMaterialId = x?.systemMaterialId
                    if (newMaterialId != null) {
                        val newChange = qtyChanges[newMaterialId] ?: Pair(0, 0)
                        qtyChanges[newMaterialId] = Pair(newChange.first, newChange.second?.plus(x.qty?.toInt() ?: 0))
                    }
                }

                // 값 업데이트
                it.qty = x?.qty?.toInt()
                it.unitPrice = x?.unitPrice?.toInt()
                it.unitVat = x?.unitVat?.toInt()
                it.totalPrice = x?.totalPrice?.toInt()
                it.systemMaterialId = x?.systemMaterialId
            }
        }

        // 변경된 인벤토리 저장
        inventoryInRep.saveAll(inventoryInList)

        // 재고 현황 업데이트
        // 품목 변경이 있는 경우 해당 품목들의 재고를 조정
        if (materialChanges.isNotEmpty() || qtyChanges.isNotEmpty()) {
            // 영향 받는 모든 품목 ID 추출
            val affectedMaterialIds = (materialChanges.values.flatMap { listOf(it.first, it.second) } +
                                     qtyChanges.keys).filterNotNull().distinct()

            // 현재 재고 상태 조회
            val currentInventoryStatus = inventoryStatusRep.findByCompCdAndSiteAndSystemMaterialIdIn(
                currentUser.compCd,
                currentUser.getSite(),
                affectedMaterialIds
            ).associateBy { it.systemMaterialId }

            // 변경 사항 반영
            val statusesToUpdate = mutableListOf<InventoryStatus>()
            val logsToSave = mutableListOf<InventoryHistory>()

            // 수량 변경 처리
            qtyChanges.forEach { (materialId, qtyChange) ->
                val oldQty = qtyChange.first ?: 0
                val newQty = qtyChange.second ?: 0
                val status = currentInventoryStatus[materialId]

                if (status != null) {
                    // 기존 재고 조정
                    val statusCurrentQty = status.qty ?: 0
                    status.qty = statusCurrentQty - oldQty + newQty
                    status.updateUser = currentUser.loginId
                    status.updateDate = LocalDateTime.now()
                    statusesToUpdate.add(status)

                    val materialInfo = getMaterialInfo(status.systemMaterialId)
                    val warehouseInfo = getWareHouseInfo(status.warehouseId)
                    val factoryInfo = getFactoryInfo(status.factoryId)

                    // 로그 추가
                    logsToSave.add(InventoryHistory().apply {
                        site = currentUser.getSite()
                        compCd = currentUser.compCd
                        warehouseName = warehouseInfo?.warehouseName
                        factoryName = factoryInfo?.factoryName
                        supplierName = materialInfo?.supplierName
                        manufacturerName = materialInfo?.manufacturerName
                        materialName = materialInfo?.materialName
                        unit = materialInfo?.unit
                        prevQty = statusCurrentQty
                        changeQty = newQty - oldQty
                        currentQty = statusCurrentQty - oldQty + newQty
                        inOutType = "UPDATE"
                        reason = "품목 수정: 수량 변경"
                        createUser = currentUser.loginId
                        createDate = LocalDateTime.now()
                    })
                } else if (newQty > 0) {
                    // 새 품목에 대한 재고 생성
                    val newStatus = InventoryStatus().apply {
                        site = currentUser.getSite()
                        compCd = currentUser.compCd
                        systemMaterialId = materialId
                        this.qty = newQty
                        flagActive = true
                        createUser = currentUser.loginId
                        createDate = LocalDateTime.now()
                        updateUser = currentUser.loginId
                        updateDate = LocalDateTime.now()
                    }
                    statusesToUpdate.add(newStatus)

                    val materialInfo = getMaterialInfo(newStatus.systemMaterialId)
                    val warehouseInfo = getWareHouseInfo(newStatus.warehouseId)
                    val factoryInfo = getFactoryInfo(newStatus.factoryId)

                    // 로그 추가
                    logsToSave.add(InventoryHistory().apply {
                        site = currentUser.getSite()
                        compCd = currentUser.compCd
                        warehouseName = warehouseInfo?.warehouseName
                        factoryName = factoryInfo?.factoryName
                        supplierName = materialInfo?.supplierName
                        manufacturerName = materialInfo?.manufacturerName
                        materialName = materialInfo?.materialName
                        unit = materialInfo?.unit
                        prevQty = 0
                        changeQty = newQty
                        currentQty = newQty
                        inOutType = "UPDATE"
                        reason = "품목 수정: 새 품목 추가"
                        createUser = currentUser.loginId
                        createDate = LocalDateTime.now()
                    })
                }
            }

            // 상태 및 로그 저장
            if (statusesToUpdate.isNotEmpty()) {
                inventoryStatusRep.saveAll(statusesToUpdate)
            }

            if (logsToSave.isNotEmpty()) {
                inventoryHistoryRep.saveAll(logsToSave)
            }
        } else {
            // 단순 수량 변경인 경우 기존 메서드 활용
            updateInventory(inventoryInList)
        }
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

        val now = LocalDateTime.now()

        // 2. 생성할 엔티티 리스트 만들기
        val inventoryList = createdRows.mapIndexedNotNull { index, input ->
            input?.let {
                // 각 항목마다 고유한 UUID 기반 ID 생성
                val randomId = UUID.randomUUID().toString().substring(0, 8)
                val newOutManagementId = "OUT$randomId"

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

        // 삭제할 출고 항목들 조회
        val inventoryOutItems = inventoryOutRep.findByOutManagementIdAndSiteAndCompCd(
            param.outManagementId,
            currentUser.getSite(),
            currentUser.compCd
        )

        // 재고 조정을 위한 준비
        if (inventoryOutItems.isNotEmpty()) {
            // 영향 받는 품목 ID 및 수량 정보 수집
            val materialQtyMap = inventoryOutItems.filter { it.systemMaterialId != null }
                .groupBy { it.systemMaterialId }
                .mapValues { entry -> entry.value.sumOf { it.qty ?: 0 } }

            // 현재 재고 상태 조회
            val materialIds = materialQtyMap.keys.filterNotNull()
            if (materialIds.isNotEmpty()) {
                val currentInventoryStatus = inventoryStatusRep.findByCompCdAndSiteAndSystemMaterialIdIn(
                    currentUser.compCd,
                    currentUser.getSite(),
                    materialIds
                ).associateBy { it.systemMaterialId }

                // 재고 조정 및 로그 생성
                val statusesToUpdate = mutableListOf<InventoryStatus>()
                val logsToSave = mutableListOf<InventoryHistory>()

                materialQtyMap.forEach { (materialId, qty) ->
                    if (materialId != null) {
                        val status = currentInventoryStatus[materialId]

                        if (status != null) {
                            // 기존 재고에서 삭제되는 출고 수량만큼 증가
                            var currentQty = status.qty ?: 0
                            status.qty = currentQty + qty  // 출고 삭제는 재고 증가
                            status.updateUser = currentUser.loginId
                            status.updateDate = LocalDateTime.now()
                            statusesToUpdate.add(status)

                            val materialInfo = getMaterialInfo(status.systemMaterialId)
                            val warehouseInfo = getWareHouseInfo(status.warehouseId)
                            val factoryInfo = getFactoryInfo(status.factoryId)

                            // 로그 추가
                            logsToSave.add(InventoryHistory().apply {
                                site = currentUser.getSite()
                                compCd = currentUser.compCd
                                warehouseName = warehouseInfo?.warehouseName
                                factoryName = factoryInfo?.factoryName
                                supplierName = materialInfo?.supplierName
                                manufacturerName = materialInfo?.manufacturerName
                                materialName = materialInfo?.materialName
                                unit = materialInfo?.unit
                                prevQty = currentQty
                                changeQty = qty  // 출고 삭제는 재고 증가
                                currentQty = currentQty + qty
                                inOutType = "DELETE"
                                reason = "출고 삭제: ${param.outManagementId}"
                                createUser = currentUser.loginId
                                createDate = LocalDateTime.now()
                            })
                        } else {
                            // 재고가 없으면 새로 생성
                            val newStatus = InventoryStatus().apply {
                                site = currentUser.getSite()
                                compCd = currentUser.compCd
                                systemMaterialId = materialId
                                this.qty = qty
                                flagActive = true
                                createUser = currentUser.loginId
                                createDate = LocalDateTime.now()
                                updateUser = currentUser.loginId
                                updateDate = LocalDateTime.now()
                            }
                            statusesToUpdate.add(newStatus)

                            val materialInfo = getMaterialInfo(newStatus.systemMaterialId)
                            val warehouseInfo = getWareHouseInfo(newStatus.warehouseId)
                            val factoryInfo = getFactoryInfo(newStatus.factoryId)

                            // 로그 추가
                            logsToSave.add(InventoryHistory().apply {
                                site = currentUser.getSite()
                                compCd = currentUser.compCd
                                warehouseName = warehouseInfo?.warehouseName
                                factoryName = factoryInfo?.factoryName
                                supplierName = materialInfo?.supplierName
                                manufacturerName = materialInfo?.manufacturerName
                                materialName = materialInfo?.materialName
                                unit = materialInfo?.unit
                                prevQty = 0
                                changeQty = qty
                                currentQty = qty
                                inOutType = "DELETE"
                                reason = "출고 삭제: ${param.outManagementId} - 재고 신규 생성"
                                createUser = currentUser.loginId
                                createDate = LocalDateTime.now()
                            })
                        }
                    }
                }

                // 상태 및 로그 저장
                if (statusesToUpdate.isNotEmpty()) {
                    inventoryStatusRep.saveAll(statusesToUpdate)
                }

                if (logsToSave.isNotEmpty()) {
                    inventoryHistoryRep.saveAll(logsToSave)
                }
            }
        }

        // 출고 관리 및 출고 항목 삭제
        inventoryOutManagementRep.deleteByOutManagementIdAndSiteAndCompCd(
            param.outManagementId,
            site = currentUser.getSite(),
            compCd = currentUser.compCd)
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

        // 삭제할 출고 항목 조회
        val inventoryOut = inventoryOutRep.findByOutInventoryIdAndSiteAndCompCd(
            param.outInventoryId,
            currentUser.getSite(),
            currentUser.compCd
        )

        // 재고 조정
        if (inventoryOut != null && inventoryOut.systemMaterialId != null) {
            var systemMaterialId = inventoryOut.systemMaterialId
            val qtyToAdd = inventoryOut.qty ?: 0 // 출고 삭제는 재고 증가

            // 현재 재고 상태 조회
            val inventoryStatus = systemMaterialId?.let {
                inventoryStatusRep.findByCompCdAndSiteAndSystemMaterialId(
                    currentUser.compCd,
                    currentUser.getSite(),
                    it
                )
            }

            if (inventoryStatus != null) {
                // 재고 증가
                var currentQty = inventoryStatus.qty ?: 0
                inventoryStatus.qty = currentQty + qtyToAdd
                inventoryStatus.updateUser = currentUser.loginId
                inventoryStatus.updateDate = LocalDateTime.now()
                inventoryStatusRep.save(inventoryStatus)

                // 로그 추가
                val log = InventoryHistory().apply {
                    site = currentUser.getSite()
                    compCd = currentUser.compCd
                    systemMaterialId = inventoryOut.systemMaterialId
                    prevQty = currentQty
                    changeQty = qtyToAdd
                    currentQty = currentQty + qtyToAdd
                    inOutType = "DELETE"
                    reason = "개별 출고 삭제: ${inventoryOut.outInventoryId}"
                    createUser = currentUser.loginId
                    createDate = LocalDateTime.now()
                }
                inventoryHistoryRep.save(log)
            }
        }

        // 출고 항목 삭제
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
        val now = LocalDateTime.now()
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

        //재고현황 업데이트
        updateInventory(inventoryOutList)
    }

    @Transactional
    fun updateDetailedOutInventory(updatedRows: List<InventoryOutUpdateInput?>){
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        val detailedOutInventoryListIds = updatedRows.map {
            it?.outInventoryId
        }

        // 기존 출고 항목 조회
        val inventoryOutList = inventoryOutRep.getDetailedInventoryOutListByIds(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            outInventoryId = detailedOutInventoryListIds
        )

        val updateList = inventoryOutList.associateBy { it?.outInventoryId }

        // 품목 변경을 추적하기 위한 맵
        val materialChanges = mutableMapOf<String?, Pair<String?, String?>>() // outInventoryId -> (oldMaterialId, newMaterialId)
        val qtyChanges = mutableMapOf<String?, Pair<Int?, Int?>>() // systemMaterialId -> (oldQty, newQty)

        // 변경 사항 추적
        updatedRows.forEach { x ->
            val outInventoryId = x?.outInventoryId
            val outInventory = updateList[outInventoryId]

            outInventory?.let {
                // 품목 변경 추적
                if (it.systemMaterialId != x?.systemMaterialId) {
                    materialChanges[outInventoryId] = Pair(it.systemMaterialId, x?.systemMaterialId)
                }

                // 수량 변경 추적
                if (it.qty != x?.qty?.toInt()) {
                    // 기존 품목에 대한 수량 변경 (출고는 재고 감소)
                    val oldMaterialId = it.systemMaterialId
                    if (oldMaterialId != null) {
                        val oldChange = qtyChanges[oldMaterialId] ?: Pair(0, 0)
                        qtyChanges[oldMaterialId] = Pair(oldChange.first?.plus(it.qty ?: 0), oldChange.second)
                    }

                    // 새 품목에 대한 수량 변경
                    val newMaterialId = x?.systemMaterialId
                    if (newMaterialId != null) {
                        val newChange = qtyChanges[newMaterialId] ?: Pair(0, 0)
                        qtyChanges[newMaterialId] = Pair(newChange.first, newChange.second?.plus(x.qty?.toInt() ?: 0))
                    }
                }

                // 값 업데이트
                it.qty = x?.qty?.toInt()
                it.unitPrice = x?.unitPrice?.toInt()
                it.unitVat = x?.unitVat?.toInt()
                it.totalPrice = x?.totalPrice?.toInt()
                it.systemMaterialId = x?.systemMaterialId
            }
        }

        // 변경된 출고 인벤토리 저장
        inventoryOutRep.saveAll(inventoryOutList)

        // 재고 현황 업데이트
        // 품목 변경이 있는 경우 해당 품목들의 재고를 조정
        if (materialChanges.isNotEmpty() || qtyChanges.isNotEmpty()) {
            // 영향 받는 모든 품목 ID 추출
            val affectedMaterialIds = (materialChanges.values.flatMap { listOf(it.first, it.second) } +
                                     qtyChanges.keys).filterNotNull().distinct()

            // 현재 재고 상태 조회
            val currentInventoryStatus = inventoryStatusRep.findByCompCdAndSiteAndSystemMaterialIdIn(
                currentUser.compCd,
                currentUser.getSite(),
                affectedMaterialIds
            ).associateBy { it.systemMaterialId }

            // 변경 사항 반영
            val statusesToUpdate = mutableListOf<InventoryStatus>()
            val logsToSave = mutableListOf<InventoryHistory>()

            // 수량 변경 처리 (출고는 재고 감소)
            qtyChanges.forEach { (materialId, qtyChange) ->
                val oldQty = qtyChange.first ?: 0
                val newQty = qtyChange.second ?: 0
                val status = currentInventoryStatus[materialId]

                if (status != null) {
                    // 기존 재고 조정 (출고는 재고에서 빼기 때문에 oldQty는 더하고 newQty는 뺌)
                    var currentQty = status.qty ?: 0
                    status.qty = currentQty + oldQty - newQty
                    status.updateUser = currentUser.loginId
                    status.updateDate = LocalDateTime.now()
                    statusesToUpdate.add(status)

                    val materialInfo = getMaterialInfo(status.systemMaterialId)
                    val warehouseInfo = getWareHouseInfo(status.warehouseId)
                    val factoryInfo = getFactoryInfo(status.factoryId)

                    // 로그 추가
                    logsToSave.add(InventoryHistory().apply {
                        site = currentUser.getSite()
                        compCd = currentUser.compCd
                        warehouseName = warehouseInfo?.warehouseName
                        factoryName = factoryInfo?.factoryName
                        supplierName = materialInfo?.supplierName
                        manufacturerName = materialInfo?.manufacturerName
                        materialName = materialInfo?.materialName
                        unit = materialInfo?.unit
                        prevQty = currentQty
                        changeQty = oldQty - newQty
                        currentQty = currentQty + oldQty - newQty
                        inOutType = "DELETE"
                        reason = "출고 수정: 수량 변경"
                        createUser = currentUser.loginId
                        createDate = LocalDateTime.now()
                    })
                } else if (newQty > 0) {
                    // 새 품목에 대한 재고 감소 (재고가 없으면 음수 재고)
                    val newStatus = InventoryStatus().apply {
                        site = currentUser.getSite()
                        compCd = currentUser.compCd
                        systemMaterialId = materialId
                        this.qty = qty
                        flagActive = true
                        createUser = currentUser.loginId
                        createDate = LocalDateTime.now()
                        updateUser = currentUser.loginId
                        updateDate = LocalDateTime.now()
                    }
                    statusesToUpdate.add(newStatus)

                    val materialInfo = getMaterialInfo(newStatus.systemMaterialId)
                    val warehouseInfo = getWareHouseInfo(newStatus.warehouseId)
                    val factoryInfo = getFactoryInfo(newStatus.factoryId)

                    // 로그 추가
                    logsToSave.add(InventoryHistory().apply {
                        site = currentUser.getSite()
                        compCd = currentUser.compCd
                        warehouseName = warehouseInfo?.warehouseName
                        factoryName = factoryInfo?.factoryName
                        supplierName = materialInfo?.supplierName
                        manufacturerName = materialInfo?.manufacturerName
                        materialName = materialInfo?.materialName
                        unit = materialInfo?.unit
                        prevQty = 0
                        changeQty = -newQty
                        currentQty = -newQty
                        inOutType = "UPDATE"
                        reason = "출고 수정: 새 품목 출고"
                        createUser = currentUser.loginId
                        createDate = LocalDateTime.now()
                    })
                }
            }
            
            // 상태 및 로그 저장
            if (statusesToUpdate.isNotEmpty()) {
                inventoryStatusRep.saveAll(statusesToUpdate)
            }
            
            if (logsToSave.isNotEmpty()) {
                inventoryHistoryRep.saveAll(logsToSave)
            }
        } else {
            // 단순 수량 변경인 경우 기존 메서드 활용
            updateInventory(inventoryOutList)
        }
    }

    //재고 현황 조회
    fun getInventoryStatusWithJoinInfo(filter: InventoryStatusFilter): List<InventoryStatusResponseModel?> {

        val currentUser = getCurrentUserPrincipal()

        return inventoryStatusRep.findInventoryStatusFiltered(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            warehouseName = filter.warehouseName ?: "",
            supplierName = filter.supplierName ?: "",
            manufacturerName = filter.manufactureName ?: "",
            materialName = filter.materialName ?: "",
        )
    }

    //재고 상세 이력 조회
    fun getInventoryHistoryList(filter: InventoryHistoryFilter?) : List<InventoryHistory?>{

        val currentUser = getCurrentUserPrincipal()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        return inventoryHistoryRep.searchInventoryHistory(
             site = currentUser.getSite(),
             compCd = currentUser.compCd,
             warehouseName = filter?.warehouseName,
             inOutType = filter?.inOutType,
             supplierName = filter?.supplierName,
             manufacturerName = filter?.manufacturerName,
             materialName = filter?.materialName,
             startDate = filter?.startDate,
             endDate = filter?.endDate,
        )
    }


    //기타 메서드
    fun getWareHouseInfo(warehouseId: String?): Warehouse? = warehouseRep.findByWarehouseId(warehouseId)
    fun getFactoryInfo(factoryId: String?): Factory? = factoryRep.findByFactoryId(factoryId)
    fun getMaterialInfo(systemMaterialId: String?): MaterialMaster? = materialMasterRep.findBySystemMaterialId(systemMaterialId)
}

//DTO
data class InventoryInManagementResponseModel(
    val inManagementId: String,
    val inType: String,
    val factoryId: String?,
    val warehouseId: String?,
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
    val systemMaterialId: String? = null,
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
    val factoryId: String?,
    val warehouseId: String?,
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
    val systemMaterialId: String? = null,
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
data class InventoryStatusResponseModel(
    val warehouseName: String?,
    val supplierName: String?,
    val manufacturerName: String?,
    val systemMaterialId: String?,
    val materialName: String?,
    val unit: String?,
    val qty: Int?
)