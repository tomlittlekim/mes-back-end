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
                .mapValues { entry -> entry.value.sumOf { it.qty ?: 0.0 } }
                
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
                            val currentQty = status.qty ?: 0.0
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
                                this.currentQty = currentQty - qty
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
            val qtyToRemove = inventoryIn.qty ?: 0.0

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
                val currentQty = inventoryStatus.qty ?: 0.0
                inventoryStatus.qty = currentQty - qtyToRemove
                inventoryStatus.updateUser = currentUser.loginId
                inventoryStatus.updateDate = LocalDateTime.now()
                inventoryStatusRep.save(inventoryStatus)

                // 로그 추가
                val log = InventoryHistory().apply {
                    site = currentUser.getSite()
                    compCd = currentUser.compCd
                    warehouseName = getWareHouseInfo(inventoryStatus.warehouseId)?.warehouseName
                    factoryName = getFactoryInfo(inventoryStatus.factoryId)?.factoryName
                    supplierName = getMaterialInfo(systemMaterialId)?.supplierName
                    manufacturerName = getMaterialInfo(systemMaterialId)?.manufacturerName
                    materialName = getMaterialInfo(systemMaterialId)?.materialName
                    unit = getMaterialInfo(systemMaterialId)?.unit
                    prevQty = currentQty
                    changeQty = -qtyToRemove
                    this.currentQty = currentQty - qtyToRemove
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

    private fun createDetailedInventory(createdRows: List<InventoryInSaveInput?>) {
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        val now = LocalDateTime.now()

        val inInventoryList = createdRows.filterNotNull().map { it ->
            // 각 항목마다 고유한 UUID 기반 ID 생성
            val randomId = UUID.randomUUID().toString().substring(0, 8)
            val newInInventoryId = "INI$randomId"

            InventoryIn().apply {
                site = currentUser.getSite()
                compCd = currentUser.compCd
                inManagementId = it.inManagementId
                inInventoryId = newInInventoryId
                systemMaterialId = it.systemMaterialId
                inType = it.inType
                qty = it.qty?.toDoubleOrNull()
                unitPrice = it.unitPrice?.toIntOrNull()
                unitVat = it.unitVat?.toIntOrNull()
                totalPrice = it.totalPrice?.toIntOrNull()
                flagActive = true
                createUser = currentUser.loginId
                createDate = now
                updateUser = currentUser.loginId
                updateDate = now
            }
        }

        val savedList = inventoryInRep.saveAll(inInventoryList)

        // 총합 계산 & 저장
        val managementIds = savedList.mapNotNull { it.inManagementId }.distinct()
        managementIds.forEach { inManagementId ->
            updateInManagementTotalPrice(inManagementId)
        }

        // 재고 현황 업데이트
        updateInventory(savedList)
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

        items.forEach { item ->
            when (item) {
                is InventoryIn -> {
                    val inQty = item.qty ?: 0.0
                    val systemMaterialId = item.systemMaterialId ?: return@forEach
                    val warehouseId = inManagementInfo?.warehouseId ?: return@forEach

                    val existingInventory = existingInventoryMap[systemMaterialId]

                    // 이력 기록을 위한 정보 수집
                    val supplierName = getMaterialInfo(systemMaterialId)?.supplierName
                    val manufacturerName = getMaterialInfo(systemMaterialId)?.manufacturerName
                    val materialName = getMaterialInfo(systemMaterialId)?.materialName
                    val unit = getMaterialInfo(systemMaterialId)?.unit

                    if (existingInventory != null) {
                        // 기존 재고가 있으면 수량만 업데이트
                        val oldQty = existingInventory.qty ?: 0.0
                        val newQty = oldQty + inQty

                        // 재고 상태 업데이트
                        existingInventory.qty = newQty
                        existingInventory.updateUser = currentUser.loginId
                        existingInventory.updateDate = LocalDateTime.now()
                        inventoryStatusToUpdate.add(existingInventory)

                        // 재고 이력 추가
                        inventoryLogsToSave.add(InventoryHistory().apply {
                            site = currentSite
                            compCd = currentCompCd
                            warehouseName = getWareHouseInfo(warehouseId)?.warehouseName
                            inOutType = "IN"
                            this.supplierName = supplierName
                            this.manufacturerName = manufacturerName
                            this.materialName = materialName
                            this.unit = unit
                            prevQty = oldQty
                            changeQty = inQty
                            currentQty = newQty
                            createUser = currentUser.loginId
                            createDate = LocalDateTime.now()
                            updateUser = currentUser.loginId
                            updateDate = LocalDateTime.now()
                            flagActive = true
                        })
                    } else {
                        // 기존 재고가 없으면 새로 생성
                        val newInventoryStatus = InventoryStatus().apply {
                            site = currentSite
                            compCd = currentCompCd
                            factoryId = inManagementInfo.factoryId
                            this.warehouseId = warehouseId
                            this.systemMaterialId = systemMaterialId
                            qty = inQty
                            createUser = currentUser.loginId
                            createDate = LocalDateTime.now()
                            updateUser = currentUser.loginId
                            updateDate = LocalDateTime.now()
                            flagActive = true
                        }
                        inventoryStatusToUpdate.add(newInventoryStatus)

                        // 재고 이력 추가
                        inventoryLogsToSave.add(InventoryHistory().apply {
                            site = currentSite
                            compCd = currentCompCd
                            warehouseName = getWareHouseInfo(warehouseId)?.warehouseName
                            inOutType = "IN"
                            this.supplierName = supplierName
                            this.manufacturerName = manufacturerName
                            this.materialName = materialName
                            this.unit = unit
                            prevQty = 0.0
                            changeQty = inQty
                            currentQty = inQty
                            createUser = currentUser.loginId
                            createDate = LocalDateTime.now()
                            updateUser = currentUser.loginId
                            updateDate = LocalDateTime.now()
                            flagActive = true
                        })
                    }
                }
                is InventoryOut -> {
                    val outQty = item.qty ?: 0.0
                    val systemMaterialId = item.systemMaterialId ?: return@forEach
                    val warehouseId = outManagementInfo?.warehouseId ?: return@forEach

                    // 해당 material의 재고 확인
                    val existingInventory = existingInventoryMap[systemMaterialId]
                        ?: throw IllegalStateException("출고할 재고가 없습니다: $systemMaterialId")

                    // 재고 충분한지 확인
                    val oldQty = existingInventory.qty ?: 0.0
                    if (oldQty < outQty) {
                        throw IllegalStateException("재고가 부족합니다: $systemMaterialId (가능: $oldQty, 요청: $outQty)")
                    }

                    // 이력 기록을 위한 정보 수집
                    val supplierName = getMaterialInfo(systemMaterialId)?.supplierName
                    val manufacturerName = getMaterialInfo(systemMaterialId)?.manufacturerName
                    val materialName = getMaterialInfo(systemMaterialId)?.materialName
                    val unit = getMaterialInfo(systemMaterialId)?.unit

                    // 재고 수량 업데이트
                    val newQty = oldQty - outQty
                    existingInventory.qty = newQty
                    existingInventory.updateUser = currentUser.loginId
                    existingInventory.updateDate = LocalDateTime.now()
                    inventoryStatusToUpdate.add(existingInventory)

                    // 재고 이력 추가
                    inventoryLogsToSave.add(InventoryHistory().apply {
                        site = currentSite
                        compCd = currentCompCd
                        warehouseName = getWareHouseInfo(warehouseId)?.warehouseName
                        inOutType = "OUT"
                        this.supplierName = supplierName
                        this.manufacturerName = manufacturerName
                        this.materialName = materialName
                        this.unit = unit
                        prevQty = oldQty
                        changeQty = outQty
                        currentQty = newQty
                        createUser = currentUser.loginId
                        createDate = LocalDateTime.now()
                        updateUser = currentUser.loginId
                        updateDate = LocalDateTime.now()
                        flagActive = true
                    })
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

        val detailedInInventoryListIds = updatedRows.mapNotNull { it?.inInventoryId }

        if (detailedInInventoryListIds.isEmpty()) {
            return // 업데이트할 항목이 없으면 종료
        }

        // 기존 인벤토리 항목 조회
        val inventoryInList = inventoryInRep.getDetailedInventoryInListByIds(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            inInventoryId = detailedInInventoryListIds
        )

        val updateMap = updatedRows.filterNotNull().associateBy { it.inInventoryId }
        val originalQtyMap = inventoryInList.associate { (it?.inInventoryId ?: "") to (it?.qty ?: 0.0) } // 원래 수량 저장

        val changedManagementIds = mutableSetOf<String>() // totalPrice 업데이트 필요한 Management ID

        inventoryInList.forEach { inventoryIn ->
            inventoryIn?.inInventoryId?.let { id ->
                updateMap[id]?.let { updateData ->
                    inventoryIn.inManagementId?.let { changedManagementIds.add(it) } // Management ID 추가

                    // 값 업데이트 로직 (기존 로직 유지 또는 필요시 수정)
                    val oldQty = inventoryIn.qty ?: 0.0
                    val newQty = updateData.qty?.toDoubleOrNull() ?: 0.0
                    val oldMaterialId = inventoryIn.systemMaterialId
                    val newMaterialId = updateData.systemMaterialId

                    inventoryIn.systemMaterialId = newMaterialId
                    inventoryIn.qty = newQty
                    inventoryIn.unitPrice = updateData.unitPrice?.toIntOrNull()
                    inventoryIn.unitVat = updateData.unitVat?.toIntOrNull()
                    inventoryIn.totalPrice = updateData.totalPrice?.toIntOrNull() // 상세 항목 totalPrice 업데이트
                    inventoryIn.updateUser = currentUser.loginId
                    inventoryIn.updateDate = LocalDateTime.now()

                    // 재고 및 이력 업데이트 로직 (수정 필요 시)
                    // updateInventoryStatusAndLogForUpdate(inventoryIn, oldQty, newQty, oldMaterialId, newMaterialId, currentUser)
                }
            }
        }

        // 변경된 인벤토리 저장
        inventoryInRep.saveAll(inventoryInList.filterNotNull())

        // 재고 현황 및 이력 업데이트 (별도 함수 호출 또는 여기에 로직 통합)
        // 이 부분은 updateInventory 함수 로직과 유사하게 처리 필요 (품목 변경, 수량 변경 고려)
        updateInventoryStatusAndLogForDetailedUpdate(inventoryInList, originalQtyMap, currentUser)

        // Management totalPrice 업데이트
        changedManagementIds.forEach { updateInManagementTotalPrice(it) }
    }

    // 재고 현황 및 이력 업데이트 로직 (상세 업데이트용)
    private fun updateInventoryStatusAndLogForDetailedUpdate(
        updatedInventoryInList: List<InventoryIn?>,
        originalQtyMap: Map<String, Double>,
        currentUser: kr.co.imoscloud.security.UserPrincipal
    ) {
        val affectedMaterialIds = mutableSetOf<String>()
        val qtyChanges = mutableMapOf<String, Pair<Double, Double>>() // materialId -> (totalOldQty, totalNewQty)

        updatedInventoryInList.filterNotNull().forEach { item ->
            val inInventoryId = item.inInventoryId ?: return@forEach
            val originalQty = originalQtyMap[inInventoryId] ?: 0.0
            val newQty = item.qty ?: 0.0
            val materialId = item.systemMaterialId ?: return@forEach // systemMaterialId 없으면 스킵

            affectedMaterialIds.add(materialId)

            val change = qtyChanges.getOrPut(materialId) { Pair(0.0, 0.0) }
            qtyChanges[materialId] = Pair(change.first + originalQty, change.second + newQty)
        }

        if (affectedMaterialIds.isEmpty()) return

        // --- 기존 updateInventory 와 유사한 로직 시작 ---
        val currentSite = currentUser.getSite()
        val currentCompCd = currentUser.compCd

        // Management 정보 조회 (필요 시)
        val inManagementInfoMap = updatedInventoryInList
            .filterNotNull()
            .mapNotNull { it.inManagementId }
            .distinct()
            .associateWith {
                inventoryInManagementRep.findByCompCdAndSiteAndInManagementId(currentCompCd, currentSite, it)
            }


        val existingInventoryList = inventoryStatusRep.findByCompCdAndSiteAndSystemMaterialIdIn(
            currentCompCd,
            currentSite,
            affectedMaterialIds.toList()
        )
        val existingInventoryMap = existingInventoryList.associateBy { it.systemMaterialId }

        val inventoryStatusToUpdate = mutableListOf<InventoryStatus>()
        val inventoryLogsToSave = mutableListOf<InventoryHistory>()
        val now = LocalDateTime.now()

        qtyChanges.forEach { (materialId, change) ->
            val totalOldQty = change.first
            val totalNewQty = change.second
            val netChange = totalNewQty - totalOldQty // 순 변화량

            if (netChange == 0.0) return@forEach // 변화 없으면 스킵

            val existingStatus = existingInventoryMap[materialId]
            val prevQty = existingStatus?.qty ?: 0.0

            if (existingStatus != null) {
                existingStatus.qty = prevQty + netChange
                existingStatus.updateUser = currentUser.loginId
                existingStatus.updateDate = now
                inventoryStatusToUpdate.add(existingStatus)
            } else if (netChange > 0) { // 재고가 없는데 증가하는 경우 (새 품목 추가 등)
                 // Management 정보 찾아야 함
                val representativeItem = updatedInventoryInList.firstOrNull { it?.systemMaterialId == materialId }
                val managementInfo = representativeItem?.inManagementId?.let { inManagementInfoMap[it] }

                val newStatus = InventoryStatus().apply {
                    site = currentSite
                    compCd = currentCompCd
                    factoryId = managementInfo?.factoryId
                    warehouseId = managementInfo?.warehouseId
                    systemMaterialId = materialId
                    qty = netChange
                    flagActive = true
                    createUser = currentUser.loginId
                    createDate = now
                    updateUser = currentUser.loginId
                    updateDate = now
                }
                inventoryStatusToUpdate.add(newStatus)
            } else {
                 // 재고가 없는데 감소하는 경우 (이론상 발생 어려움 or 음수 재고 허용 시 처리)
                 println("Warning: Trying to decrease stock for non-existing material $materialId")
                 // 필요 시 음수 재고 처리 로직 추가
            }

             // 로그 생성
            val representativeItem = updatedInventoryInList.firstOrNull { it?.systemMaterialId == materialId } // 대표 항목 정보 사용
            val managementInfo = representativeItem?.inManagementId?.let { inManagementInfoMap[it] }
            val materialInfo = getMaterialInfo(materialId)
            val warehouseInfo = getWareHouseInfo(managementInfo?.warehouseId ?: existingStatus?.warehouseId)
            val factoryInfo = getFactoryInfo(managementInfo?.factoryId ?: existingStatus?.factoryId)


            inventoryLogsToSave.add(InventoryHistory().apply {
                site = currentSite
                compCd = currentCompCd
                warehouseName = warehouseInfo?.warehouseName
                factoryName = factoryInfo?.factoryName
                supplierName = materialInfo?.supplierName
                manufacturerName = materialInfo?.manufacturerName
                materialName = materialInfo?.materialName
                unit = materialInfo?.unit
                this.prevQty = prevQty
                changeQty = netChange
                currentQty = prevQty + netChange
                inOutType = "UPDATE" // 또는 "IN_UPDATE" 등 구분
                reason = "입고 수정: ${representativeItem?.inManagementId ?: "N/A"}"
                createUser = currentUser.loginId
                createDate = now
            })
        }


        if (inventoryStatusToUpdate.isNotEmpty()) {
            inventoryStatusRep.saveAll(inventoryStatusToUpdate)
        }
        if (inventoryLogsToSave.isNotEmpty()) {
            inventoryHistoryRep.saveAll(inventoryLogsToSave)
        }
        // --- 기존 updateInventory 와 유사한 로직 끝 ---
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
        val nonNullUpdatedRows = updatedRows ?: emptyList() // Null이면 빈 리스트 사용
        createDetailedOutInventory(createdRows)
        updateDetailedOutInventory(nonNullUpdatedRows)
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
                .mapValues { entry -> entry.value.sumOf { it.qty ?: 0.0 } }

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
                            val currentQty = status.qty ?: 0.0
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
                                changeQty = qty
                                this.currentQty = currentQty + qty
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
                                prevQty = 0.0
                                changeQty = qty
                                this.currentQty = qty
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
        ) ?: return // 삭제할 항목 없으면 종료

        val outManagementId = inventoryOut.outManagementId
        val qtyToAdd = inventoryOut.qty ?: 0.0 // 출고 삭제는 재고 증가
        val systemMaterialId = inventoryOut.systemMaterialId

        // 재고 조정
        if (systemMaterialId != null) {
            // 현재 재고 상태 조회
            val inventoryStatus = inventoryStatusRep.findByCompCdAndSiteAndSystemMaterialId(
                    currentUser.compCd,
                    currentUser.getSite(),
                    systemMaterialId
                )

            if (inventoryStatus != null) {
                // 재고 증가
                val currentQty = inventoryStatus.qty ?: 0.0
                inventoryStatus.qty = currentQty + qtyToAdd
                inventoryStatus.updateUser = currentUser.loginId
                inventoryStatus.updateDate = LocalDateTime.now()
                inventoryStatusRep.save(inventoryStatus)

                 // Management 정보 조회 (로그용)
                val managementInfo = outManagementId?.let { inventoryOutManagementRep.findByOutManagementIdAndSiteAndCompCd(it, currentUser.getSite(), currentUser.compCd) }
                val materialInfo = getMaterialInfo(systemMaterialId)
                val warehouseInfo = getWareHouseInfo(managementInfo?.warehouseId ?: inventoryStatus.warehouseId)
                val factoryInfo = getFactoryInfo(managementInfo?.factoryId ?: inventoryStatus.factoryId)

                // 로그 추가
                val log = InventoryHistory().apply {
                    site = currentUser.getSite()
                    compCd = currentUser.compCd
                    warehouseName = warehouseInfo?.warehouseName
                    factoryName = factoryInfo?.factoryName
                    supplierName = materialInfo?.supplierName
                    manufacturerName = materialInfo?.manufacturerName
                    materialName = materialInfo?.materialName
                    unit = materialInfo?.unit
                    prevQty = currentQty
                    changeQty = qtyToAdd // 출고 삭제는 양수
                    this.currentQty = currentQty + qtyToAdd
                    inOutType = "DELETE" // 또는 "OUT_DELETE"
                    reason = "개별 출고 삭제: ${inventoryOut.outInventoryId}"
                    createUser = currentUser.loginId
                    createDate = LocalDateTime.now()
                }
                inventoryHistoryRep.save(log)
            } else {
                 // 재고가 없는데 출고가 삭제된 경우 (음수 재고였거나 오류 상황)
                 // 필요 시 재고를 생성하고 양수 값으로 설정
                 val managementInfo = outManagementId?.let { inventoryOutManagementRep.findByOutManagementIdAndSiteAndCompCd(it, currentUser.getSite(), currentUser.compCd) }
                 val newStatus = InventoryStatus().apply {
                    site = currentUser.getSite()
                    compCd = currentUser.compCd
                    factoryId = managementInfo?.factoryId
                    warehouseId = managementInfo?.warehouseId
                    this.systemMaterialId = systemMaterialId
                    qty = qtyToAdd // 삭제된 출고량만큼 재고 생성
                    flagActive = true
                    createUser = currentUser.loginId
                    createDate = LocalDateTime.now()
                    updateUser = currentUser.loginId
                    updateDate = LocalDateTime.now()
                }
                 inventoryStatusRep.save(newStatus)
                 println("Info: Created new inventory status for material $systemMaterialId due to OUT delete.")
                  // 로그 추가 (재고 신규 생성)
                 val materialInfo = getMaterialInfo(systemMaterialId)
                 val warehouseInfo = getWareHouseInfo(newStatus.warehouseId)
                 val factoryInfo = getFactoryInfo(newStatus.factoryId)

                 val log = InventoryHistory().apply {
                     site = currentUser.getSite()
                     compCd = currentUser.compCd
                     warehouseName = warehouseInfo?.warehouseName
                     factoryName = factoryInfo?.factoryName
                     supplierName = materialInfo?.supplierName
                     manufacturerName = materialInfo?.manufacturerName
                     materialName = materialInfo?.materialName
                     unit = materialInfo?.unit
                     prevQty = 0.0
                     changeQty = qtyToAdd
                     this.currentQty = qtyToAdd
                     inOutType = "DELETE"
                     reason = "개별 출고 삭제 (재고 생성): ${inventoryOut.outInventoryId}"
                     createUser = currentUser.loginId
                     createDate = LocalDateTime.now()
                 }
                 inventoryHistoryRep.save(log)
            }
        }

        // 출고 항목 삭제 실행
        inventoryOutRep.deleteByOutInventoryIdAndSiteAndCompCd(
            param.outInventoryId,
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
        )

        // Management totalPrice 업데이트 (삭제 후 호출)
        outManagementId?.let { updateOutManagementTotalPrice(it) }
    }

    private fun createDetailedOutInventory(createdRows: List<InventoryOutSaveInput?>) {
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        val now = LocalDateTime.now()

        val outInventoryList = createdRows.filterNotNull().map { it ->
            // 각 항목마다 고유한 UUID 기반 ID 생성
            val randomId = UUID.randomUUID().toString().substring(0, 8)
            val newOutInventoryId = "OUTI$randomId"

            InventoryOut().apply {
                site = currentUser.getSite()
                compCd = currentUser.compCd
                outManagementId = it.outManagementId
                outInventoryId = newOutInventoryId
                systemMaterialId = it.systemMaterialId
                outType = it.outType
                qty = it.qty?.toDoubleOrNull() // 안전한 형변환
                unitPrice = it.unitPrice?.toIntOrNull()
                unitVat = it.unitVat?.toIntOrNull()
                totalPrice = it.totalPrice?.toIntOrNull()
                flagActive = true
                createUser = currentUser.loginId
                createDate = now
                updateUser = currentUser.loginId
                updateDate = now
            }
        }

        val savedList = inventoryOutRep.saveAll(outInventoryList)

        // 총합 계산 & 저장
        val managementIds = savedList.mapNotNull { it.outManagementId }.distinct()
        managementIds.forEach { outManagementId ->
            updateOutManagementTotalPrice(outManagementId)
        }

        // 재고 현황 업데이트
        updateInventory(savedList)
    }

    @Transactional
    fun updateDetailedOutInventory(updatedRows: List<InventoryOutUpdateInput?>){
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        val detailedOutInventoryListIds = updatedRows.mapNotNull { it?.outInventoryId }

        if (detailedOutInventoryListIds.isEmpty()) {
            return // 업데이트할 항목 없으면 종료
        }


        // 기존 출고 항목 조회
        val inventoryOutList = inventoryOutRep.getDetailedInventoryOutListByIds(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            outInventoryId = detailedOutInventoryListIds
        )

        val updateMap = updatedRows.filterNotNull().associateBy { it.outInventoryId }
        val originalQtyMap = inventoryOutList.associate { (it?.outInventoryId ?: "") to (it?.qty ?: 0.0) } // 원래 수량 저장


        val changedManagementIds = mutableSetOf<String>() // totalPrice 업데이트 필요한 Management ID

        inventoryOutList.forEach { inventoryOut ->
            inventoryOut?.outInventoryId?.let { id ->
                updateMap[id]?.let { updateData ->
                    inventoryOut.outManagementId?.let { changedManagementIds.add(it) } // Management ID 추가

                     // 값 업데이트 로직 (기존 로직 유지 또는 필요시 수정)
                    val oldQty = inventoryOut.qty ?: 0.0
                    val newQty = updateData.qty?.toDoubleOrNull() ?: 0.0
                    val oldMaterialId = inventoryOut.systemMaterialId
                    val newMaterialId = updateData.systemMaterialId

                    inventoryOut.systemMaterialId = newMaterialId
                    inventoryOut.qty = newQty
                    inventoryOut.unitPrice = updateData.unitPrice?.toIntOrNull()
                    inventoryOut.unitVat = updateData.unitVat?.toIntOrNull()
                    inventoryOut.totalPrice = updateData.totalPrice?.toIntOrNull() // 상세 항목 totalPrice 업데이트
                    inventoryOut.updateUser = currentUser.loginId
                    inventoryOut.updateDate = LocalDateTime.now()

                     // 재고 및 이력 업데이트 로직 (수정 필요 시)
                     // updateInventoryStatusAndLogForOutUpdate(inventoryOut, oldQty, newQty, oldMaterialId, newMaterialId, currentUser)
                }
            }
        }

        // 변경된 출고 인벤토리 저장
        inventoryOutRep.saveAll(inventoryOutList.filterNotNull())

        // 재고 현황 및 이력 업데이트 (별도 함수 호출 또는 여기에 로직 통합)
        updateInventoryStatusAndLogForDetailedOutUpdate(inventoryOutList, originalQtyMap, currentUser)


        // Management totalPrice 업데이트
        changedManagementIds.forEach { updateOutManagementTotalPrice(it) }
    }

     // 재고 현황 및 이력 업데이트 로직 (출고 상세 업데이트용)
     private fun updateInventoryStatusAndLogForDetailedOutUpdate(
         updatedInventoryOutList: List<InventoryOut?>,
         originalQtyMap: Map<String, Double>,
         currentUser: kr.co.imoscloud.security.UserPrincipal
     ) {
         val affectedMaterialIds = mutableSetOf<String>()
         val qtyChanges = mutableMapOf<String, Pair<Double, Double>>() // materialId -> (totalOldQty, totalNewQty) - 출고 기준

         updatedInventoryOutList.filterNotNull().forEach { item ->
             val outInventoryId = item.outInventoryId ?: return@forEach
             val originalQty = originalQtyMap[outInventoryId] ?: 0.0 // 이전 출고량
             val newQty = item.qty ?: 0.0 // 새 출고량
             val materialId = item.systemMaterialId ?: return@forEach

             affectedMaterialIds.add(materialId)

             val change = qtyChanges.getOrPut(materialId) { Pair(0.0, 0.0) }
             qtyChanges[materialId] = Pair(change.first + originalQty, change.second + newQty)
         }

         if (affectedMaterialIds.isEmpty()) return

         // --- 재고 및 이력 업데이트 로직 (출고 버전) ---
         val currentSite = currentUser.getSite()
         val currentCompCd = currentUser.compCd

         // Management 정보 조회 (필요 시)
         val outManagementInfoMap = updatedInventoryOutList
             .filterNotNull()
             .mapNotNull { it.outManagementId }
             .distinct()
             .associateWith {
                 inventoryOutManagementRep.findByOutManagementIdAndSiteAndCompCd(it, currentSite, currentCompCd)
             }


         val existingInventoryList = inventoryStatusRep.findByCompCdAndSiteAndSystemMaterialIdIn(
             currentCompCd,
             currentSite,
             affectedMaterialIds.toList()
         )
         val existingInventoryMap = existingInventoryList.associateBy { it.systemMaterialId }

         val inventoryStatusToUpdate = mutableListOf<InventoryStatus>()
         val inventoryLogsToSave = mutableListOf<InventoryHistory>()
         val now = LocalDateTime.now()

         qtyChanges.forEach { (materialId, change) ->
             val totalOldOutQty = change.first
             val totalNewOutQty = change.second
             val netChange = totalOldOutQty - totalNewOutQty // 재고 변화량 (이전 출고량 - 새 출고량)

             if (netChange == 0.0) return@forEach

             val existingStatus = existingInventoryMap[materialId]
             val prevQty = existingStatus?.qty ?: 0.0 // 이전 재고

             if (existingStatus != null) {
                 existingStatus.qty = prevQty + netChange
                 existingStatus.updateUser = currentUser.loginId
                 existingStatus.updateDate = now
                 inventoryStatusToUpdate.add(existingStatus)
             } else {
                 // 재고가 없는데 출고가 변경되는 경우 (음수 재고였거나, 새 품목 출고 등)
                 val representativeItem = updatedInventoryOutList.firstOrNull { it?.systemMaterialId == materialId }
                 val managementInfo = representativeItem?.outManagementId?.let { outManagementInfoMap[it] }

                 val newStatus = InventoryStatus().apply {
                     site = currentSite
                     compCd = currentCompCd
                     factoryId = managementInfo?.factoryId
                     warehouseId = managementInfo?.warehouseId
                     systemMaterialId = materialId
                     qty = netChange // 재고 변화량만큼 설정 (음수일 수 있음)
                     flagActive = true
                     createUser = currentUser.loginId
                     createDate = now
                     updateUser = currentUser.loginId
                     updateDate = now
                 }
                 inventoryStatusToUpdate.add(newStatus)
                 println("Info: Created/Updated inventory status for material $materialId with change $netChange due to OUT update.")
             }

             // 로그 생성
             val representativeItem = updatedInventoryOutList.firstOrNull { it?.systemMaterialId == materialId }
             val managementInfo = representativeItem?.outManagementId?.let { outManagementInfoMap[it] }
             val materialInfo = getMaterialInfo(materialId)
             val warehouseInfo = getWareHouseInfo(managementInfo?.warehouseId ?: existingStatus?.warehouseId)
             val factoryInfo = getFactoryInfo(managementInfo?.factoryId ?: existingStatus?.factoryId)


             inventoryLogsToSave.add(InventoryHistory().apply {
                 site = currentSite
                 compCd = currentCompCd
                 warehouseName = warehouseInfo?.warehouseName
                 factoryName = factoryInfo?.factoryName
                 supplierName = materialInfo?.supplierName
                 manufacturerName = materialInfo?.manufacturerName
                 materialName = materialInfo?.materialName
                 unit = materialInfo?.unit
                 this.prevQty = prevQty
                 changeQty = netChange // 재고 변화량 기록
                 currentQty = prevQty + netChange
                 inOutType = "UPDATE" // 또는 "OUT_UPDATE"
                 reason = "출고 수정: ${representativeItem?.outManagementId ?: "N/A"}"
                 createUser = currentUser.loginId
                 createDate = now
             })
         }

         if (inventoryStatusToUpdate.isNotEmpty()) {
             inventoryStatusRep.saveAll(inventoryStatusToUpdate)
         }
         if (inventoryLogsToSave.isNotEmpty()) {
             inventoryHistoryRep.saveAll(inventoryLogsToSave)
         }
         // --- 재고 및 이력 업데이트 로직 (출고 버전) 끝 ---
     }

    //재고 현황 조회
    fun getInventoryStatusWithJoinInfo(filter: InventoryStatusFilter): List<InventoryStatusResponseModel?> {

        val currentUser = getCurrentUserPrincipal()

        return inventoryStatusRep.findInventoryStatusFiltered(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            warehouseName = filter.warehouseName ?: "",
            supplierName = filter.supplierName ?: "",
            manufacturerName = filter.manufacturerName ?: "",
            materialName = filter.materialName ?: "",
        )
    }

    //재고 상세 이력 조회
    fun getInventoryHistoryList(filter: InventoryHistoryFilter?) : List<InventoryHistory?>{

        val materialNameList = filter?.materialNames?.filterNotNull()

        val currentUser = getCurrentUserPrincipal()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        return inventoryHistoryRep.searchInventoryHistory(
             site = currentUser.getSite(),
             compCd = currentUser.compCd,
             warehouseName = filter?.warehouseName,
             inOutType = filter?.inOutType,
             supplierName = filter?.supplierName,
             manufacturerName = filter?.manufacturerName,
             materialNames = materialNameList,
             startDate = filter?.startDate,
             endDate = filter?.endDate,
        )
    }


    //기타 메서드
    fun getWareHouseInfo(warehouseId: String?): Warehouse? = warehouseRep.findByWarehouseId(warehouseId)
    fun getFactoryInfo(factoryId: String?): Factory? = factoryRep.findByFactoryId(factoryId)
    fun getMaterialInfo(systemMaterialId: String?): MaterialMaster? = materialMasterRep.findBySystemMaterialId(systemMaterialId)

    // --- Private helper functions for updating management total price ---
    private fun updateInManagementTotalPrice(inManagementId: String) {
        val currentUser = getCurrentUserPrincipalOrNull() ?: return // 사용자 정보 없으면 종료

        val management = inventoryInManagementRep.findByCompCdAndSiteAndInManagementId(
            currentUser.compCd,
            currentUser.getSite(),
            inManagementId
        ) ?: return // Management 정보 없으면 종료

        val total = inventoryInRep.calculateTotalSumByInManagementId(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            inManagementId = inManagementId
        )?.toInt() ?: 0 // 합계가 null이면 0으로 처리

        management.totalPrice = total
        management.updateUser = currentUser.loginId
        management.updateDate = LocalDateTime.now()
        inventoryInManagementRep.save(management)
    }

    private fun updateOutManagementTotalPrice(outManagementId: String) {
        val currentUser = getCurrentUserPrincipalOrNull() ?: return

        val management = inventoryOutManagementRep.findByOutManagementIdAndSiteAndCompCd(
            outManagementId,
            currentUser.getSite(),
            currentUser.compCd
        ) ?: return

        val total = inventoryOutRep.calculateTotalSumByOutManagementId(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            outManagementId = outManagementId
        )?.toInt() ?: 0

        management.totalPrice = total
        management.updateUser = currentUser.loginId
        management.updateDate = LocalDateTime.now()
        inventoryOutManagementRep.save(management)
    }

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
    val qty: Double?
)