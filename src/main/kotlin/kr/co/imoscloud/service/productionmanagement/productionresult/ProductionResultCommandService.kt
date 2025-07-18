package kr.co.imoscloud.service.productionmanagement.productionresult

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.exception.productionmanagement.ProductionQtyExceededException
import kr.co.imoscloud.model.productionmanagement.DefectInfoInput
import kr.co.imoscloud.model.productionmanagement.ProductionResultInput
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.service.productionmanagement.DefectInfoService
import kr.co.imoscloud.util.DateUtils.parseDateTimeFromString
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 생산실적 생성/삭제 관련 서비스
 */
@Service
class ProductionResultCommandService(
    private val productionResultRepository: ProductionResultRepository,
    private val workOrderRepository: WorkOrderRepository,
    private val defectInfoService: DefectInfoService,
    private val productionResultQueryService: ProductionResultQueryService,
    private val productionInventoryService: ProductionInventoryService
) {
    /**
     * 생산실적 저장 (생성)
     * - 불량정보도 함께 저장할 수 있는 기능 추가
     * - 양품수량 검증 로직 추가 (작업지시수량 초과 불가)
     */
    @Transactional
    fun saveProductionResult(
        createdRows: List<ProductionResultInput>? = null
    ): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()

            // 작업지시별 누적 수량을 추적하기 위한 맵
            val workOrderAccumulatedQty = mutableMapOf<String, Double>()

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
                        val workOrderId = workOrder.workOrderId!!
                        
                        // 현재 작업지시에 대해 이미 처리된 누적 수량 가져오기
                        val currentAccumulatedQty = workOrderAccumulatedQty.getOrDefault(workOrderId, 0.0)
                        
                        // 총 수량 = 기존 등록된 수량 + 현재 배치에서 이미 처리된 수량 + 현재 행의 수량
                        val totalGoodQty = existingTotalGoodQty + currentAccumulatedQty + goodQty
                        
                        if (totalGoodQty > orderQty) {
                            throw ProductionQtyExceededException()
                        }
                        
                        // 현재 작업지시의 누적 수량 업데이트
                        workOrderAccumulatedQty[workOrderId] = currentAccumulatedQty + goodQty
                    }

                    // 수정된 진척률 계산 - 작업지시가 있는 경우에만 계산
                    val progressRate = if (workOrder != null && workOrder.orderQty != null && workOrder.orderQty!! > 0) {
                        // 기존 생산실적의 양품수량 + 현재 생산실적의 양품수량으로 누적 진척률 계산
                        val currentAccumulatedQty = workOrderAccumulatedQty.getOrDefault(workOrder.workOrderId!!, 0.0)
                        String.format("%.1f", ((existingTotalGoodQty + currentAccumulatedQty) / workOrder.orderQty!!) * 100.0)
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
                        warehouseId = input.warehouseId
                        resultInfo = input.resultInfo
                        defectCause = input.defectCause
                        prodStartTime = parseDateTimeFromString(input.prodStartTime)
                        prodEndTime = input.prodEndTime?.let { parseDateTimeFromString(it) }
                        flagActive = true
                        createCommonCol(currentUser)
                    }

                    val savedResult = productionResultRepository.save(newResult)

                    // 생산한 제품 ID로 BOM 찾기
                    input.productId?.let { productId ->
                        // 재고 관련 처리를 ProductionInventoryService로 위임
                        // 자재 소비 처리
                        productionInventoryService.processProductionMaterialConsumption(
                            productId = productId, 
                            productionQty = goodQty.toInt(), 
                            site = currentUser.getSite(), 
                            compCd = currentUser.compCd
                        )
                        
                        // 생산품 재고 증가 처리
                        if (goodQty > 0) {
                            productionInventoryService.increaseProductInventory(
                                productId = productId,
                                productionQty = goodQty,
                                site = currentUser.getSite(),
                                compCd = currentUser.compCd,
                                warehouseId = input.warehouseId
                            )
                        }
                    }

                    // 해당 생산실적에 연결된 불량정보가 있는 경우 함께 저장 (개선사항)
                    if (!input.defectInfos.isNullOrEmpty()) {
                        // 각 불량정보에 prodResultId를 명시적으로 설정
                        val updatedDefectInfos = input.defectInfos!!.map { defectInfo ->
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
                        val currentAccumulatedQty = workOrderAccumulatedQty.getOrDefault(workOrder.workOrderId!!, 0.0)
                        updateWorkOrderStatus(workOrder, currentAccumulatedQty, existingTotalGoodQty, workOrder.orderQty ?: 0.0, totalQty)
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
        currentBatchAccumulatedQty: Double,
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
            // 해당 작업지시의 총 생산량 계산 (기존 수량 + 현재 배치에서 누적된 수량)
            val totalWorkOrderGoodQty = existingTotalGoodQty + currentBatchAccumulatedQty

            // 총 생산량이 작업지시수량에 도달하면 완료 상태로 변경
            if (orderQty > 0 && totalWorkOrderGoodQty >= orderQty) {
                workOrder.state = "COMPLETED"
                workOrder.updateCommonCol(currentUser)
                workOrderRepository.save(workOrder)
            }
        }
    }

    /**
     * 생산실적을 다중 소프트 삭제하는 메서드 (flagActive = false로 설정)
     * - 연관된 불량정보도 함께 비활성화 처리
     * - QueryDSL을 이용한 배치 처리로 DB 통신 최소화
     */
    @Transactional
    fun softDeleteProductionResults(prodResultIds: List<String>): Boolean {
        try {
            if (prodResultIds.isEmpty()) return false
            
            val currentUser = getCurrentUserPrincipal()
            
            // 1. 삭제할 생산실적들을 한 번에 조회 (재고 복원을 위해) - JPA Query Method 사용
            val existingResults = productionResultRepository.findBySiteAndCompCdAndProdResultIdInAndFlagActive(
                site = currentUser.getSite(),
                compCd = currentUser.compCd,
                prodResultId = prodResultIds,
                flagActive = true
            )
            
            if (existingResults.isEmpty()) return false
            
            // 2. 재고 복원 처리 (각 생산실적별로 개별 처리 필요)
            existingResults.forEach { result ->
                try {
                    if (result.goodQty != null && result.goodQty!! > 0.0 && result.productId != null) {
                        productionInventoryService.restoreInventoryForDeletedProductionResult(result)
                    }
                } catch (e: Exception) {
                    // 재고 복원 실패 시 로그만 남기고 계속 진행
                    println("재고 복원 실패: ${result.prodResultId} - ${e.message}")
                }
            }
            
            // 3. 생산실적 배치 소프트 삭제 (saveAll 방식 - 안전하고 디버깅 용이)
            existingResults.forEach { result ->
                result.softDelete(currentUser)
            }
            val savedResults = productionResultRepository.saveAll(existingResults)
            val deletedProductionCount = savedResults.size
            
            // 4. 연관된 불량정보 배치 소프트 삭제 (saveAll 방식 - 안전하고 디버깅 용이)
            val deletedDefectCount = defectInfoService.batchSoftDeleteDefectInfosByProdResultIds(
                prodResultIds = prodResultIds
            )
            
            println("배치 삭제 완료 - 생산실적: ${deletedProductionCount}건, 불량정보: ${deletedDefectCount}건")
            
            return deletedProductionCount > 0
        } catch (e: Exception) {
            println("배치 삭제 중 오류 발생: ${e.message}")
            return false
        }
    }
}