package kr.co.imoscloud.service.productionmanagement.productionresult

import kr.co.imoscloud.entity.inventory.InventoryHistory
import kr.co.imoscloud.entity.inventory.InventoryStatus
import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.model.productionmanagement.DefectInfoInput
import kr.co.imoscloud.model.productionmanagement.ProductionResultInput
import kr.co.imoscloud.model.productionmanagement.ProductionResultUpdate
import kr.co.imoscloud.repository.inventory.InventoryHistoryRep
import kr.co.imoscloud.repository.inventory.InventoryStatusRep
import kr.co.imoscloud.repository.material.BomDetailRepository
import kr.co.imoscloud.repository.material.BomRepository
import kr.co.imoscloud.repository.material.MaterialRepository
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.service.productionmanagement.DefectInfoService
import kr.co.imoscloud.util.DateUtils.parseDateTimeFromString
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 생산실적 생성/수정/삭제 관련 서비스
 */
@Service
class ProductionResultCommandService(
    private val productionResultRepository: ProductionResultRepository,
    private val workOrderRepository: WorkOrderRepository,
    private val defectInfoService: DefectInfoService,
    private val productionResultQueryService: ProductionResultQueryService,
    private val bomRepository: BomRepository,
    private val bomDetailRepository: BomDetailRepository,
    private val inventoryStatusRep: InventoryStatusRep,
    private val inventoryHistoryRep: InventoryHistoryRep,
    private val materialRepository: MaterialRepository
) {
    /**
     * 생산실적 저장 (생성/수정)
     * - 불량정보도 함께 저장할 수 있는 기능 추가
     * - 양품수량 검증 로직 추가 (작업지시수량 초과 불가)
     */
    @Transactional
    fun saveProductionResult(
        createdRows: List<ProductionResultInput>? = null,
        updatedRows: List<ProductionResultUpdate>? = null,
        defectInfos: List<DefectInfoInput>? = null
    ): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()

            // 새로운 생산실적 저장
            createdRows?.forEach { input ->
                try {
                    // 작업지시 정보 조회 (선택 사항으로 변경)
                    val workOrder = input.workOrderId?.let {
                        workOrderRepository.findBySiteAndCompCdAndWorkOrderId(
                            site = currentUser.getSite(),
                            compCd = currentUser.compCd,
                            workOrderId = it
                        )
                    }

                    // 작업지시가 있는 경우에만 기존 등록된 양품수량 합계 조회
                    val existingTotalGoodQty = if (workOrder != null) {
                        productionResultQueryService.getTotalGoodQtyByWorkOrderId(workOrder.workOrderId!!)
                    } else 0.0

                    // 양품과 불량품 수량 준비
                    val goodQty = input.goodQty ?: 0.0
                    val defectQty = input.defectQty ?: 0.0
                    val totalQty = goodQty + defectQty

                    // 작업지시가 있는 경우에만 작업지시 수량 검증
                    if (workOrder != null) {
                        val orderQty = workOrder.orderQty ?: 0.0
                        if (existingTotalGoodQty + goodQty > orderQty) {
                            throw IllegalArgumentException("총 생산 양품수량이 작업지시수량(${orderQty})을 초과할 수 없습니다. 현재 등록된 양품수량: ${existingTotalGoodQty}")
                        }
                    }

                    // 수정된 진척률 계산 - 작업지시가 있는 경우에만 계산
                    val progressRate = if (workOrder != null && workOrder.orderQty != null && workOrder.orderQty!! > 0) {
                        // 기존 생산실적의 양품수량 + 현재 생산실적의 양품수량으로 누적 진척률 계산
                        String.format("%.1f", ((existingTotalGoodQty + goodQty) / workOrder.orderQty!!) * 100.0)
                    } else "0.0"

                    // 불량률 계산 - 불량수량이 있는 경우만 계산
                    val defectRate = if (totalQty > 0) {
                        String.format("%.1f", (defectQty / totalQty) * 100.0)
                    } else "0.0"

                    // 생산실적 ID 생성 - 중복 방지를 위해 타임스탬프와 랜덤값 조합
                    val timestamp = System.currentTimeMillis()
                    val random = (Math.random() * 1000).toInt()
                    val prodResultId = "PR$timestamp-$random"

                    val newResult = ProductionResult().apply {
                        site = currentUser.getSite()
                        compCd = currentUser.compCd
                        this.prodResultId = prodResultId
                        workOrderId = input.workOrderId // null 허용
                        productId = input.productId
                        this.goodQty = goodQty
                        this.defectQty = defectQty
                        this.progressRate = progressRate
                        this.defectRate = defectRate
                        equipmentId = input.equipmentId
                        resultInfo = input.resultInfo
                        defectCause = input.defectCause
                        prodStartTime = parseDateTimeFromString(input.prodStartTime)
                        prodEndTime = parseDateTimeFromString(input.prodEndTime)
                        flagActive = true
                        createCommonCol(currentUser)
                    }

                    val savedResult = productionResultRepository.save(newResult)

                    // 생산한 제품 ID로 BOM 찾기
                    input.productId?.let { productId ->
                        processProductionMaterialConsumption(productId, goodQty.toInt(), currentUser.getSite(), currentUser.compCd)
                    }

                    // 불량정보가 있는 경우 함께 저장
                    val relatedDefectInfos = defectInfos

                    if (!relatedDefectInfos.isNullOrEmpty()) {
                        // 각 불량정보에 prodResultId를 명시적으로 설정
                        val updatedDefectInfos = relatedDefectInfos.map { defectInfo ->
                            // prodResultId가 없거나 다른 경우 업데이트
                            if (defectInfo.prodResultId.isNullOrBlank() || defectInfo.prodResultId != savedResult.prodResultId) {
                                defectInfo.copy(prodResultId = savedResult.prodResultId)
                            } else {
                                defectInfo
                            }
                        }

                        defectInfoService.saveDefectInfoForProductionResult(
                            prodResultId = savedResult.prodResultId!!,
                            defectInputs = updatedDefectInfos
                        )
                    }

                    // 작업지시가 있는 경우에만 작업지시 상태 업데이트
                    if (workOrder != null) {
                        updateWorkOrderStatus(workOrder, goodQty, existingTotalGoodQty, workOrder.orderQty ?: 0.0, totalQty)
                    }
                } catch (e: Exception) {
                    throw e  // 트랜잭션 롤백을 위해 예외를 다시 던짐
                }
            }

            // 기존 생산실적 업데이트
            updatedRows?.forEach { update ->
                try {
                    // 특정 조건에 맞는 생산실적을 직접 찾는 쿼리 사용
                    val existingResult = productionResultRepository.findBySiteAndCompCdAndProdResultId(
                        currentUser.getSite(),
                        currentUser.compCd,
                        update.prodResultId
                    ) ?: throw IllegalArgumentException("수정할 생산실적을 찾을 수 없습니다: ${update.prodResultId}")

                    // 작업지시 정보 조회 (선택 사항으로 변경)
                    val workOrder = existingResult.workOrderId?.let {
                        workOrderRepository.findBySiteAndCompCdAndWorkOrderId(
                            site = currentUser.getSite(),
                            compCd = currentUser.compCd,
                            workOrderId = it
                        )
                    }

                    // 기존 양품수량
                    val oldGoodQty = existingResult.goodQty ?: 0.0
                    // 새 양품수량
                    val newGoodQty = update.goodQty ?: oldGoodQty

                    // 작업지시가 있는 경우에만 다른 생산실적의 양품수량 합계 조회
                    var otherGoodQty = 0.0
                    var orderQty = 0.0

                    if (workOrder != null) {
                        otherGoodQty = productionResultQueryService.getTotalGoodQtyByWorkOrderId(workOrder.workOrderId!!) - oldGoodQty
                        orderQty = workOrder.orderQty ?: 0.0

                        // 양품수량 검증 - 변경 후 총 양품수량이 작업지시수량을 초과하는지 확인
                        if (otherGoodQty + newGoodQty > orderQty) {
                            throw IllegalArgumentException("총 생산 양품수량이 작업지시수량(${orderQty})을 초과할 수 없습니다. 현재 등록된 양품수량: ${otherGoodQty}")
                        }
                    }

                    // Qty 관련 계산
                    val defectQty = update.defectQty ?: existingResult.defectQty ?: 0.0
                    val totalQty = newGoodQty + defectQty

                    // 수정된 진척률 계산 - 작업지시가 있는 경우에만 계산
                    val progressRate = if (workOrder != null && orderQty > 0) {
                        // 다른 생산실적의 양품수량 + 현재 생산실적의 양품수량으로 누적 진척률 계산
                        String.format("%.1f", ((otherGoodQty + newGoodQty) / orderQty) * 100.0)
                    } else "0.0"

                    // 불량률 계산 - 현재 생산실적의 양품과 불량만 고려
                    val defectRate = if (totalQty > 0) {
                        String.format("%.1f", (defectQty / totalQty) * 100.0)
                    } else "0.0"

                    // 생산실적 업데이트
                    existingResult.apply {
                        update.workOrderId?.let { workOrderId = it }
                        update.productId?.let { productId = it }
                        update.goodQty?.let { this.goodQty = it }
                        update.defectQty?.let { this.defectQty = it }
                        this.progressRate = progressRate
                        this.defectRate = defectRate
                        update.equipmentId?.let { equipmentId = it }
                        update.resultInfo?.let { resultInfo = it }
                        update.defectCause?.let { defectCause = it }
                        update.prodStartTime?.let { prodStartTime = parseDateTimeFromString(it) }
                        update.prodEndTime?.let { prodEndTime = parseDateTimeFromString(it) }
                        updateCommonCol(currentUser)
                    }

                    val savedResult = productionResultRepository.save(existingResult)

                    // 불량정보가 있는 경우 함께 처리
                    val relatedDefectInfos = defectInfos?.filter {
                        it.prodResultId == update.prodResultId
                    }

                    if (!relatedDefectInfos.isNullOrEmpty()) {
                        // 기존 불량정보 조회
                        val existingDefectInfos = defectInfoService.getDefectInfoByProdResultId(update.prodResultId)

                        if (existingDefectInfos.isNotEmpty()) {
                            // 기존 불량정보는 모두 비활성화
                            existingDefectInfos.forEach { defectInfo ->
                                defectInfoService.softDeleteDefectInfo(defectInfo.defectId!!)
                            }
                        }

                        // 각 불량정보에 prodResultId를 명시적으로 설정
                        val updatedDefectInfos = relatedDefectInfos.map { defectInfo ->
                            // prodResultId가 없거나 다른 경우 업데이트
                            if (defectInfo.prodResultId.isNullOrBlank() || defectInfo.prodResultId != savedResult.prodResultId) {
                                defectInfo.copy(prodResultId = savedResult.prodResultId)
                            } else {
                                defectInfo
                            }
                        }

                        // 새 불량정보 추가
                        defectInfoService.saveDefectInfoForProductionResult(
                            prodResultId = savedResult.prodResultId!!,
                            defectInputs = updatedDefectInfos
                        )
                    }

                    // 작업지시가 있는 경우에만 작업지시 상태 업데이트
                    if (workOrder != null) {
                        updateWorkOrderStatus(workOrder, newGoodQty, otherGoodQty, orderQty, totalQty)
                    }
                } catch (e: Exception) {
                    throw e  // 트랜잭션 롤백을 위해 예외를 다시 던짐
                }
            }

            return true
        } catch (e: Exception) {
            throw e  // 트랜잭션 롤백을 위해 예외를 다시 던짐
        }
    }

    /**
     * 작업지시 상태 업데이트 헬퍼 메서드
     */
    private fun updateWorkOrderStatus(
        workOrder: kr.co.imoscloud.entity.productionmanagement.WorkOrder,
        goodQty: Double,
        existingTotalGoodQty: Double,
        orderQty: Double,
        totalQty: Double
    ) {
        val currentUser = getCurrentUserPrincipal()

        if (workOrder.state == "PLANNED" && totalQty > 0) {
            workOrder.state = "IN_PROGRESS"
            workOrder.updateCommonCol(currentUser)
            workOrderRepository.save(workOrder)
        } else if (workOrder.state == "IN_PROGRESS") {
            // 해당 작업지시의 총 생산량 계산
            val totalWorkOrderGoodQty = existingTotalGoodQty + goodQty

            // 총 생산량이 작업지시수량에 도달하면 완료 상태로 변경
            if (orderQty > 0 && totalWorkOrderGoodQty >= orderQty) {
                workOrder.state = "COMPLETED"
                workOrder.updateCommonCol(currentUser)
                workOrderRepository.save(workOrder)
            }
        }
    }

    /**
     * 생산실적을 소프트 삭제하는 메서드 (flagActive = false로 설정)
     * - 연관된 불량정보도 함께 비활성화 처리
     */
    @Transactional
    fun softDeleteProductionResult(prodResultId: String): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()

            // 특정 조건에 맞는 생산실적을 직접 찾는 쿼리 사용
            val existingResult = productionResultRepository.findBySiteAndCompCdAndProdResultId(
                currentUser.getSite(),
                currentUser.compCd,
                prodResultId
            )

            existingResult?.let {
                // flagActive를 false로 설정
                it.flagActive = false
                it.updateCommonCol(currentUser)
                productionResultRepository.save(it)

                // 관련 불량정보도 비활성화 처리
                val defectInfos = defectInfoService.getDefectInfoByProdResultId(prodResultId)
                defectInfos.forEach { defectInfo ->
                    defectInfoService.softDeleteDefectInfo(defectInfo.defectId!!)
                }

                return true
            }

            return false
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 생산시 자재 소비 처리 메서드
     * BOM 구성에 따른 재고 차감 및 이력 생성
     */
    private fun processProductionMaterialConsumption(
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
}