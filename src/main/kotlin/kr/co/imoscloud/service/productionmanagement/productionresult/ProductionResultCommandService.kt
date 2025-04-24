package kr.co.imoscloud.service.productionmanagement.productionresult

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
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
        createdRows: List<ProductionResultInput>? = null,
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
                        warehouseId = input.warehouseId
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
}