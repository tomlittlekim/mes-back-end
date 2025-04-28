package kr.co.imoscloud.service.productionmanagement.productionresult

import kr.co.imoscloud.entity.inventory.InventoryHistory
import kr.co.imoscloud.entity.inventory.InventoryStatus
import kr.co.imoscloud.repository.WarehouseRep
import kr.co.imoscloud.repository.inventory.InventoryHistoryRep
import kr.co.imoscloud.repository.inventory.InventoryStatusRep
import kr.co.imoscloud.repository.material.BomDetailRepository
import kr.co.imoscloud.repository.material.BomRepository
import kr.co.imoscloud.repository.material.MaterialRepository
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 생산 관련 재고 처리 서비스
 * 생산 시 원자재 소비 및 완제품 입고 처리를 담당
 */
@Service
class ProductionInventoryService(
    private val bomRepository: BomRepository,
    private val bomDetailRepository: BomDetailRepository,
    private val inventoryStatusRep: InventoryStatusRep,
    private val inventoryHistoryRep: InventoryHistoryRep,
    private val materialRepository: MaterialRepository,
    private val warehouseRep: WarehouseRep
) {
    /**
     * 생산시 자재 소비 처리 메서드
     * BOM 구성에 따른 재고 차감 및 이력 생성
     */
    @Transactional
    fun processProductionMaterialConsumption(
        productId: String, 
        productionQty: Int,
        site: String,
        compCd: String
    ) {
        // 현재 사용자 정보 가져오기
        val currentUser = getCurrentUserPrincipal()
        
        // 1. 제품 ID(productId)로 BOM 찾기 - itemCd가 productId와 일치하는 BOM 조회
        val bom = bomRepository.getBomList(
            site = site,
            compCd = compCd,
            materialType = "",
            materialName = "",
            bomName = ""
        ).find { it.bom.itemCd == productId }?.bom ?: return
        
        // 2. BOM의 상세 자재 리스트 가져오기
        val bomDetails = bomDetailRepository.getBomDetailListByBomId(site, compCd, bom.bomId ?: return)
        if (bomDetails.isEmpty()) return
        
        // 3. 사용할 모든 자재 ID 수집
        val materialIds = bomDetails.mapNotNull { it.bomDetail.itemCd }
        
        // 4. 현재 자재 재고 상태 조회
        val currentInventoryStatus = inventoryStatusRep.findByCompCdAndSiteAndSystemMaterialIdIn(
            compCd, site, materialIds
        ).associateBy { it.systemMaterialId }
        
        // 5. 업데이트할 재고 상태 및 생성할 이력 로그 준비
        val statusesToUpdate = mutableListOf<InventoryStatus>()
        val logsToSave = mutableListOf<InventoryHistory>()
        val now = LocalDateTime.now()
        
        // 6. 각 BOM 자재별로 소비량 계산 및 재고 차감 처리
        bomDetails.forEach { bomDetail ->
            val materialId = bomDetail.bomDetail.itemCd ?: return@forEach
            // Double 타입 그대로 계산하고 소수점 유지
            val requiredQtyDouble = (bomDetail.bomDetail.itemQty ?: 0.0) * productionQty.toDouble()
            // 정수형 변환은 재고 차감 시에만 적용
            val requiredQty = requiredQtyDouble
            
            if (requiredQtyDouble <= 0.0) return@forEach
            
            val existingStatus = currentInventoryStatus[materialId]
            
            // MaterialMaster에서 자재 정보 조회 (supplier, manufacturer 정보 포함)
            val materialMaster = materialRepository.findByCompCdAndSiteAndSystemMaterialId(
                compCd, site, materialId
            )
            
            if (existingStatus != null) {
                // 기존 재고가 있는 경우 - 기존 수량에 관계없이 항상 차감
                val prevQty = existingStatus.qty ?: 0.0
                val currentQty = prevQty - requiredQty
                
                existingStatus.qty = currentQty
                existingStatus.updateDate = now
                existingStatus.updateUser = currentUser.loginId // 실제 사용자 ID로 변경
                
                statusesToUpdate.add(existingStatus)
                
                // 재고 변동 이력 생성 - 소수점 정보 포함
                createInventoryConsumptionLog(
                    site = site,
                    compCd = compCd,
                    materialId = materialId,
                    materialName = bomDetail.materialName,
                    materialStandard = bomDetail.materialStandard,
                    unit = bomDetail.unit,
                    supplierName = materialMaster?.supplierName,
                    manufacturerName = materialMaster?.manufacturerName,
                    prevQty = prevQty,
                    changeQty = -requiredQty,
                    currentQty = currentQty,
                    reason = "생산소비: $productId (수량: $productionQty, 소요량: ${bomDetail.bomDetail.itemQty} × $productionQty = $requiredQtyDouble)",
                    logsToSave = logsToSave,
                    userId = currentUser.loginId
                )
            } else {
                // 재고가 없는 경우 새로 생성 (0부터 시작하여 차감)
                val newStatus = InventoryStatus().apply {
                    this.site = site
                    this.compCd = compCd
                    this.systemMaterialId = materialId
                    this.qty = -requiredQty // 0에서 차감하여 음수가 됨
                    this.createDate = now
                    this.updateDate = now
                    this.createUser = currentUser.loginId // 실제 사용자 ID
                    this.updateUser = currentUser.loginId // 실제 사용자 ID
                    this.flagActive = true
                }
                
                statusesToUpdate.add(newStatus)
                
                // 재고 변동 이력 생성 - 소수점 정보 포함
                createInventoryConsumptionLog(
                    site = site,
                    compCd = compCd,
                    materialId = materialId,
                    materialName = bomDetail.materialName,
                    materialStandard = bomDetail.materialStandard,
                    unit = bomDetail.unit,
                    supplierName = materialMaster?.supplierName,
                    manufacturerName = materialMaster?.manufacturerName,
                    prevQty = 0.0, // 초기값은 0
                    changeQty = -requiredQty,
                    currentQty = -requiredQty,
                    reason = "생산소비(재고없음): $productId (수량: $productionQty, 소요량: ${bomDetail.bomDetail.itemQty} × $productionQty = $requiredQtyDouble)",
                    logsToSave = logsToSave,
                    userId = currentUser.loginId
                )
            }
        }
        
        // 7. 재고 상태 및 이력 로그 저장
        if (statusesToUpdate.isNotEmpty()) {
            inventoryStatusRep.saveAll(statusesToUpdate)
        }
        
        if (logsToSave.isNotEmpty()) {
            inventoryHistoryRep.saveAll(logsToSave)
        }
    }
    
    /**
     * 생산품 재고 증가 처리 메서드
     * 생산된 제품의 재고를 증가시키고 이력을 남김
     */
    @Transactional
    fun increaseProductInventory(
        productId: String,
        productionQty: Double,
        site: String,
        compCd: String,
        warehouseId: String?
    ) {
        val currentUser = getCurrentUserPrincipal()
        val now = LocalDateTime.now()
        
        // 제품 정보 조회
        val productMaster = materialRepository.findByCompCdAndSiteAndSystemMaterialId(
            compCd, site, productId
        )
        
        // 제품 ID로 현재 재고 상태 조회
        val productInventoryStatus = inventoryStatusRep.findByCompCdAndSiteAndSystemMaterialId(
            compCd, site, productId
        )
        
        // 창고명 조회
        val warehouseName = getWarehouseName(warehouseId)
        
        // 재고 이력 생성용 리스트
        val logsToSave = mutableListOf<InventoryHistory>()
        
        if (productInventoryStatus != null) {
            // 기존 재고가 있는 경우 - 생산 수량만큼 재고 증가
            val prevQty = productInventoryStatus.qty ?: 0.0
            val currentQty = prevQty + productionQty
            
            productInventoryStatus.qty = currentQty
            productInventoryStatus.updateDate = now
            productInventoryStatus.updateUser = currentUser.loginId
            
            // 재고 상태 업데이트
            inventoryStatusRep.save(productInventoryStatus)
            
            // 재고 변동 이력 생성
            logsToSave.add(InventoryHistory().apply {
                this.site = site
                this.compCd = compCd
                this.warehouseName = warehouseName
                this.materialName = productMaster?.materialName
                this.unit = productMaster?.unit
                this.supplierName = productMaster?.supplierName
                this.manufacturerName = productMaster?.manufacturerName
                this.prevQty = prevQty
                this.changeQty = productionQty
                this.currentQty = currentQty
                this.inOutType = "IN"
                this.reason = "생산입고: $productId (수량: $productionQty)"
                this.flagActive = true
                this.createDate = now
                this.createUser = currentUser.loginId
                this.updateDate = now
                this.updateUser = currentUser.loginId
            })
        } else {
            // 재고가 없는 경우 새로 생성
            val newStatus = InventoryStatus().apply {
                this.site = site
                this.compCd = compCd
                this.systemMaterialId = productId
                this.warehouseId = warehouseId
                this.qty = productionQty
                this.createDate = now
                this.updateDate = now
                this.createUser = currentUser.loginId
                this.updateUser = currentUser.loginId
                this.flagActive = true
            }
            
            // 재고 상태 생성
            inventoryStatusRep.save(newStatus)
            
            // 재고 변동 이력 생성
            logsToSave.add(InventoryHistory().apply {
                this.site = site
                this.compCd = compCd
                this.warehouseName = warehouseName
                this.materialName = productMaster?.materialName
                this.unit = productMaster?.unit
                this.supplierName = productMaster?.supplierName
                this.manufacturerName = productMaster?.manufacturerName
                this.prevQty = 0.0
                this.changeQty = productionQty
                this.currentQty = productionQty
                this.inOutType = "IN"
                this.reason = "생산입고(재고없음): $productId (수량: $productionQty)"
                this.flagActive = true
                this.createDate = now
                this.createUser = currentUser.loginId
                this.updateDate = now
                this.updateUser = currentUser.loginId
            })
        }
        
        // 재고 이력 저장
        if (logsToSave.isNotEmpty()) {
            inventoryHistoryRep.saveAll(logsToSave)
        }
    }
    
    /**
     * 재고 소비 이력 생성 헬퍼
     */
    private fun createInventoryConsumptionLog(
        site: String,
        compCd: String,
        materialId: String,
        materialName: String?,
        materialStandard: String?,
        unit: String?,
        supplierName: String?,
        manufacturerName: String?,
        prevQty: Double,
        changeQty: Double,
        currentQty: Double,
        reason: String,
        logsToSave: MutableList<InventoryHistory>,
        userId: String?
    ) {
        val now = LocalDateTime.now()
        
        logsToSave.add(InventoryHistory().apply {
            this.site = site
            this.compCd = compCd
            this.materialName = materialName
            this.unit = unit
            this.supplierName = supplierName
            this.manufacturerName = manufacturerName
            this.prevQty = prevQty
            this.changeQty = changeQty
            this.currentQty = currentQty
            this.inOutType = "OUT"
            this.reason = reason
            this.flagActive = true
            this.createDate = now
            this.createUser = userId
            this.updateDate = now
            this.updateUser = userId
        })
    }
    
    /**
     * 창고 이름 조회 헬퍼 메서드
     * 창고 ID로 창고명을 조회한다
     */
    private fun getWarehouseName(warehouseId: String?): String? {
        if (warehouseId.isNullOrBlank()) return null
        
        try {
            // 창고 ID로 창고 정보 조회
            val warehouse = warehouseRep.findByWarehouseId(warehouseId)
            return warehouse?.warehouseName
        } catch (e: Exception) {
            // 오류 발생 시 로그 기록 후 null 반환
            return null
        }
    }
    
    /**
     * 생산실적 삭제 시 소비된 자재수량 및 생산된 제품 수량 복원 처리
     * @param prodResult 삭제할 생산실적 정보
     * @return 성공 여부
     */
    @Transactional
    fun restoreInventoryForDeletedProductionResult(prodResult: kr.co.imoscloud.entity.productionmanagement.ProductionResult): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()
            val site = currentUser.getSite()
            val compCd = currentUser.compCd
            
            // 생산실적에서 필요한 정보 추출
            val productId = prodResult.productId ?: return false
            val goodQty = prodResult.goodQty ?: 0.0
            
            if (goodQty <= 0.0) return true // 양품 수량이 없으면 복원할 필요 없음
            
            // 1. 제품 ID로 BOM 찾기
            val bom = bomRepository.getBomList(
                site = site,
                compCd = compCd,
                materialType = "",
                materialName = "",
                bomName = ""
            ).find { it.bom.itemCd == productId }?.bom ?: return false
            
            // 2. BOM의 상세 자재 리스트 가져오기
            val bomDetails = bomDetailRepository.getBomDetailListByBomId(site, compCd, bom.bomId ?: return false)
            if (bomDetails.isEmpty()) return false
            
            // 3. 사용할 모든 자재 ID 수집
            val materialIds = bomDetails.mapNotNull { it.bomDetail.itemCd }
            
            // 4. 현재 자재 재고 상태 조회
            val currentInventoryStatus = inventoryStatusRep.findByCompCdAndSiteAndSystemMaterialIdIn(
                compCd, site, materialIds
            ).associateBy { it.systemMaterialId }
            
            // 5. 업데이트할 재고 상태 및 생성할 이력 로그 준비
            val statusesToUpdate = mutableListOf<InventoryStatus>()
            val logsToSave = mutableListOf<InventoryHistory>()
            val now = LocalDateTime.now()
            
            // 6. 각 BOM 자재별로 소비량 계산 및 재고 복원 처리
            bomDetails.forEach { bomDetail ->
                val materialId = bomDetail.bomDetail.itemCd ?: return@forEach
                // Double 타입 그대로 계산하고 소수점 유지
                val restoredQtyDouble = (bomDetail.bomDetail.itemQty ?: 0.0) * goodQty
                // 양수로 변환 (복원이므로 더하기)
                val restoredQty = restoredQtyDouble
                
                if (restoredQtyDouble <= 0.0) return@forEach
                
                val existingStatus = currentInventoryStatus[materialId]
                
                // MaterialMaster에서 자재 정보 조회
                val materialMaster = materialRepository.findByCompCdAndSiteAndSystemMaterialId(
                    compCd, site, materialId
                )
                
                if (existingStatus != null) {
                    // 기존 재고가 있는 경우 - 복원 수량만큼 재고 증가
                    val prevQty = existingStatus.qty ?: 0.0
                    val currentQty = prevQty + restoredQty
                    
                    existingStatus.qty = currentQty
                    existingStatus.updateDate = now
                    existingStatus.updateUser = currentUser.loginId
                    
                    statusesToUpdate.add(existingStatus)
                    
                    // 재고 변동 이력 생성 - 소수점 정보 포함
                    createInventoryConsumptionLog(
                        site = site,
                        compCd = compCd,
                        materialId = materialId,
                        materialName = bomDetail.materialName,
                        materialStandard = bomDetail.materialStandard,
                        unit = bomDetail.unit,
                        supplierName = materialMaster?.supplierName,
                        manufacturerName = materialMaster?.manufacturerName,
                        prevQty = prevQty,
                        changeQty = restoredQty, // 양수로 설정 (복원이므로)
                        currentQty = currentQty,
                        reason = "생산취소 자재복원: $productId (수량: $goodQty, 복원량: ${bomDetail.bomDetail.itemQty} × $goodQty = $restoredQtyDouble)",
                        logsToSave = logsToSave,
                        userId = currentUser.loginId
                    )
                } else {
                    // 재고가 없는 경우 새로 생성
                    val newStatus = InventoryStatus().apply {
                        this.site = site
                        this.compCd = compCd
                        this.systemMaterialId = materialId
                        this.qty = restoredQty // 복원 수량으로 시작
                        this.createDate = now
                        this.updateDate = now
                        this.createUser = currentUser.loginId
                        this.updateUser = currentUser.loginId
                        this.flagActive = true
                    }
                    
                    statusesToUpdate.add(newStatus)
                    
                    // 재고 변동 이력 생성
                    createInventoryConsumptionLog(
                        site = site,
                        compCd = compCd,
                        materialId = materialId,
                        materialName = bomDetail.materialName,
                        materialStandard = bomDetail.materialStandard,
                        unit = bomDetail.unit,
                        supplierName = materialMaster?.supplierName,
                        manufacturerName = materialMaster?.manufacturerName,
                        prevQty = 0.0,
                        changeQty = restoredQty, // 양수로 설정 (복원이므로)
                        currentQty = restoredQty,
                        reason = "생산취소 자재복원(재고없음): $productId (수량: $goodQty, 복원량: ${bomDetail.bomDetail.itemQty} × $goodQty = $restoredQtyDouble)",
                        logsToSave = logsToSave,
                        userId = currentUser.loginId
                    )
                }
            }
            
            // 7. 생산된 제품 재고 차감 처리
            decreaseProductInventory(
                productId = productId,
                productionQty = goodQty,
                site = site,
                compCd = compCd,
                warehouseId = prodResult.warehouseId
            )
            
            // 8. 재고 상태 및 이력 로그 저장
            if (statusesToUpdate.isNotEmpty()) {
                inventoryStatusRep.saveAll(statusesToUpdate)
            }
            
            if (logsToSave.isNotEmpty()) {
                inventoryHistoryRep.saveAll(logsToSave)
            }
            
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * 생산실적 삭제 시 제품 재고 차감 처리 메서드
     * 생산된 제품의 재고를 감소시키고 이력을 남김
     */
    @Transactional
    fun decreaseProductInventory(
        productId: String,
        productionQty: Double,
        site: String,
        compCd: String,
        warehouseId: String?
    ) {
        val currentUser = getCurrentUserPrincipal()
        val now = LocalDateTime.now()
        
        // 제품 정보 조회
        val productMaster = materialRepository.findByCompCdAndSiteAndSystemMaterialId(
            compCd, site, productId
        )
        
        // 제품 ID로 현재 재고 상태 조회
        val productInventoryStatus = inventoryStatusRep.findByCompCdAndSiteAndSystemMaterialId(
            compCd, site, productId
        ) ?: return // 재고 없으면 처리할 필요 없음
        
        // 현재 재고가 차감할 수량보다 작은 경우 처리 (마이너스 재고 허용)
        val prevQty = productInventoryStatus.qty ?: 0.0
        val currentQty = prevQty - productionQty
        
        // 창고명 조회
        val warehouseName = getWarehouseName(warehouseId)
        
        // 재고 업데이트
        productInventoryStatus.qty = currentQty
        productInventoryStatus.updateDate = now
        productInventoryStatus.updateUser = currentUser.loginId
        
        // 재고 상태 업데이트
        inventoryStatusRep.save(productInventoryStatus)
        
        // 재고 이력 생성
        val inventoryHistory = InventoryHistory().apply {
            this.site = site
            this.compCd = compCd
            this.warehouseName = warehouseName
            this.materialName = productMaster?.materialName
            this.unit = productMaster?.unit
            this.supplierName = productMaster?.supplierName
            this.manufacturerName = productMaster?.manufacturerName
            this.prevQty = prevQty
            this.changeQty = -productionQty // 차감이므로 음수
            this.currentQty = currentQty
            this.inOutType = "OUT"
            this.reason = "생산취소 출고: $productId (수량: $productionQty)"
            this.flagActive = true
            this.createDate = now
            this.createUser = currentUser.loginId
            this.updateDate = now
            this.updateUser = currentUser.loginId
        }
        
        inventoryHistoryRep.save(inventoryHistory)
    }
} 