package kr.co.imoscloud.service.productionmanagement.productionresult

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.model.productionmanagement.DefectInfoInput
import kr.co.imoscloud.model.productionmanagement.ProductionResultInput
import kr.co.imoscloud.model.productionmanagement.ProductionResultUpdate
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.service.productionmanagement.DefectInfoService
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 생산실적 생성/수정/삭제 관련 서비스
 */
@Service
class ProductionResultCommandService(
    private val productionResultRepository: ProductionResultRepository,
    private val workOrderRepository: WorkOrderRepository,
    private val defectInfoService: DefectInfoService,
    private val productionResultQueryService: ProductionResultQueryService
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
                    // 작업지시 정보 조회
                    val workOrder = input.workOrderId?.let {
                        workOrderRepository.findBySiteAndCompCdAndWorkOrderId(
                            site = currentUser.getSite(),
                            compCd = currentUser.compCd,
                            workOrderId = it
                        )
                    } ?: throw IllegalArgumentException("작업지시를 찾을 수 없습니다: ${input.workOrderId}")

                    // 기존 등록된 양품수량 합계 조회
                    val existingTotalGoodQty = productionResultQueryService.getTotalGoodQtyByWorkOrderId(workOrder.workOrderId!!)

                    // 양품과 불량품 수량 준비
                    val goodQty = input.goodQty ?: 0.0
                    val defectQty = input.defectQty ?: 0.0
                    val totalQty = goodQty + defectQty

                    // 작업지시 수량 검증
                    val orderQty = workOrder.orderQty ?: 0.0
                    if (existingTotalGoodQty + goodQty > orderQty) {
                        throw IllegalArgumentException("총 생산 양품수량이 작업지시수량(${orderQty})을 초과할 수 없습니다. 현재 등록된 양품수량: ${existingTotalGoodQty}")
                    }

                    // 수정된 진척률 계산 - 양품수량만 사용하고 누적 값 고려
                    val progressRate = if (orderQty > 0) {
                        // 기존 생산실적의 양품수량 + 현재 생산실적의 양품수량으로 누적 진척률 계산
                        String.format("%.1f", ((existingTotalGoodQty + goodQty) / orderQty) * 100.0)
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
                        workOrderId = input.workOrderId
                        this.goodQty = goodQty
                        this.defectQty = defectQty
                        this.progressRate = progressRate
                        this.defectRate = defectRate
                        equipmentId = input.equipmentId
                        resultInfo = input.resultInfo
                        defectCause = input.defectCause
                        flagActive = true
                        createCommonCol(currentUser)
                    }

                    val savedResult = productionResultRepository.save(newResult)

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

                    // 작업지시 상태 업데이트
                    updateWorkOrderStatus(workOrder, goodQty, existingTotalGoodQty, orderQty, totalQty)
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

                    // 작업지시 정보 조회
                    val workOrder = existingResult.workOrderId?.let {
                        workOrderRepository.findBySiteAndCompCdAndWorkOrderId(
                            site = currentUser.getSite(),
                            compCd = currentUser.compCd,
                            workOrderId = it
                        )
                    } ?: throw IllegalArgumentException("작업지시를 찾을 수 없습니다: ${existingResult.workOrderId}")

                    // 기존 양품수량
                    val oldGoodQty = existingResult.goodQty ?: 0.0
                    // 새 양품수량
                    val newGoodQty = update.goodQty ?: oldGoodQty
                    // 현재 편집중인 생산실적을 제외한 다른 생산실적의 양품수량 합계
                    val otherGoodQty = productionResultQueryService.getTotalGoodQtyByWorkOrderId(workOrder.workOrderId!!) - oldGoodQty

                    // Qty 관련 계산
                    val orderQty = workOrder.orderQty ?: 0.0
                    val defectQty = update.defectQty ?: existingResult.defectQty ?: 0.0
                    val totalQty = newGoodQty + defectQty

                    // 양품수량 검증 - 변경 후 총 양품수량이 작업지시수량을 초과하는지 확인
                    if (otherGoodQty + newGoodQty > orderQty) {
                        throw IllegalArgumentException("총 생산 양품수량이 작업지시수량(${orderQty})을 초과할 수 없습니다. 현재 등록된 양품수량: ${otherGoodQty}")
                    }

                    // 수정된 진척률 계산 - 양품수량만 사용하고 누적 값 고려
                    val progressRate = if (orderQty > 0) {
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
                        update.goodQty?.let { this.goodQty = it }
                        update.defectQty?.let { this.defectQty = it }
                        this.progressRate = progressRate
                        this.defectRate = defectRate
                        update.equipmentId?.let { equipmentId = it }
                        update.resultInfo?.let { resultInfo = it }
                        update.defectCause?.let { defectCause = it }
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

                    // 작업지시 상태 업데이트
                    updateWorkOrderStatus(workOrder, newGoodQty, otherGoodQty, orderQty, totalQty)
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