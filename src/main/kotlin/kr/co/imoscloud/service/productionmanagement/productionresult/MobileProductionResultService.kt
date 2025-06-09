package kr.co.imoscloud.service.productionmanagement.productionresult

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.exception.productionmanagement.ProductionResultAlreadyCompletedException
import kr.co.imoscloud.exception.productionmanagement.ProductionResultNotFoundException
import kr.co.imoscloud.model.productionmanagement.DefectInfoInput
import kr.co.imoscloud.model.productionmanagement.ProductionResultFilter
import kr.co.imoscloud.model.productionmanagement.ProductionResultInput
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.service.productionmanagement.DefectInfoService
import kr.co.imoscloud.util.DateUtils.parseDateTimeFromString
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 모바일 전용 생산실적 서비스
 * - 모바일에서 생산시작/종료 관련 기능을 처리
 * - 작업지시와 연동되지 않음
 */
@Service
class MobileProductionResultService(
    private val productionResultRepository: ProductionResultRepository,
    private val defectInfoService: DefectInfoService,
    private val productionInventoryService: ProductionInventoryService
) {
    private val log = LoggerFactory.getLogger(MobileProductionResultService::class.java)

    /**
     * 모바일에서 진행 중인 생산실적 목록 조회
     * - 생산이 시작되었으나 종료되지 않은(prodEndTime이 null) 데이터를 조회
     */
    fun getProductionResultsAtMobile(filter: ProductionResultFilter?): List<ProductionResult> {
        val currentUser = getCurrentUserPrincipal()
        return productionResultRepository.getProductionResultsAtMobile(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            filter = filter
        )
    }

    /**
     * 모바일 전용 생산실적 생성 (생산시작)
     * - 모바일에서 생산시작 시 호출됨
     * - 작업지시와 연동되지 않음
     * - 초기에는 productId, equipmentId, warehouseId, prodStartTime 필드만 설정
     */
    @Transactional
    fun saveProductionResultAtMobile(input: ProductionResultInput): String {
        try {
            val currentUser = getCurrentUserPrincipal()
            
            // 생산실적 ID 생성 - 중복 방지를 위해 타임스탬프와 랜덤값 조합
            val timestamp = System.currentTimeMillis()
            val random = (Math.random() * 1000).toInt()
            val prodResultId = "PR$timestamp-$random"
            
            val newResult = ProductionResult().apply {
                site = currentUser.getSite()
                compCd = currentUser.compCd
                this.prodResultId = prodResultId
                workOrderId = null // 모바일에서는 작업지시와 연동되지 않음
                productId = input.productId
                goodQty = 0.0 // 초기에는 0
                defectQty = 0.0 // 초기에는 0
                progressRate = "0.0" // 작업지시가 없으므로 항상 0
                defectRate = "0.0" // 초기에는 0
                equipmentId = input.equipmentId
                warehouseId = input.warehouseId
                resultInfo = input.resultInfo
                defectCause = null
                prodStartTime = parseDateTimeFromString(input.prodStartTime)
                prodEndTime = input.prodEndTime?.let { parseDateTimeFromString(it) } // 생산종료 시간이 제공되면 파싱, 아니면 null
                flagActive = true
                createCommonCol(currentUser)
            }
            
            productionResultRepository.save(newResult)
            
            log.info("모바일에서 생산실적 생성 완료: prodResultId={}, productId={}", prodResultId, input.productId)
            
            return prodResultId
        } catch (e: Exception) {
            log.error("모바일 생산실적 생성 중 오류 발생", e)
            throw e  // 트랜잭션 롤백을 위해 예외를 다시 던짐
        }
    }
    
    /**
     * 모바일 전용 생산실적 업데이트 (생산종료)
     * - 생산시작 시 생성된 데이터를 생산종료 시 업데이트
     * - 불량정보도 함께 저장할 수 있는 기능 추가
     */
    @Transactional
    fun updateProductionResultAtMobile(
        prodResultId: String,
        input: ProductionResultInput,
        defectInfos: List<DefectInfoInput>? = null
    ): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()
            
            // 기존 생산실적 엔티티 조회
            val existingResult = productionResultRepository.findBySiteAndCompCdAndProdResultId(
                site = currentUser.getSite(),
                compCd = currentUser.compCd,
                prodResultId = prodResultId
            ) ?: throw ProductionResultNotFoundException()
            
            // 이미 생산종료된 데이터인지 확인
            if (existingResult.prodEndTime != null) {
                throw ProductionResultAlreadyCompletedException()
            }
            
            // 양품과 불량품 수량 준비
            val goodQty = input.goodQty ?: 0.0
            val defectQty = input.defectQty ?: 0.0
            val totalQty = goodQty + defectQty
            
            // 불량률 계산 - 불량수량이 있는 경우만 계산
            val defectRate = if (totalQty > 0) {
                String.format("%.1f", (defectQty / totalQty) * 100.0)
            } else "0.0"
            
            // 기존 생산실적 엔티티 업데이트
            existingResult.apply {
                this.goodQty = goodQty
                this.defectQty = defectQty
                this.progressRate = "0.0" // 모바일에서는 작업지시가 없으므로 0으로 설정
                this.defectRate = defectRate
                // equipmentId와 warehouseId는 이미 저장되어 있으므로 변경하지 않음
                resultInfo = input.resultInfo
                defectCause = input.defectCause
                prodEndTime = input.prodEndTime?.let { parseDateTimeFromString(it) }
                updateCommonCol(currentUser)
            }
            
            // 변경사항 저장
            productionResultRepository.save(existingResult)
            
            // 생산한 제품 ID로 BOM 찾기
            existingResult.productId?.let { productId ->
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
                        warehouseId = existingResult.warehouseId
                    )
                }
            }
            
            // 불량정보가 있는 경우 함께 저장
            if (!defectInfos.isNullOrEmpty()) {
                // 각 불량정보에 prodResultId를 명시적으로 설정
                val updatedDefectInfos = defectInfos.map { defectInfo ->
                    // prodResultId가 없거나 다른 경우 업데이트
                    if (defectInfo.prodResultId.isNullOrBlank() || defectInfo.prodResultId != existingResult.prodResultId) {
                        defectInfo.copy(prodResultId = existingResult.prodResultId)
                    } else {
                        defectInfo
                    }
                }
                
                defectInfoService.saveDefectInfoForProductionResult(
                    prodResultId = existingResult.prodResultId!!,
                    defectInputs = updatedDefectInfos
                )
            }
            
            log.info("모바일에서 생산실적 업데이트 완료: prodResultId={}, productId={}, goodQty={}, defectQty={}", 
                prodResultId, existingResult.productId, goodQty, defectQty)
            
            return true
        } catch (e: Exception) {
            log.error("모바일 생산실적 업데이트 중 오류 발생", e)
            throw e  // 트랜잭션 롤백을 위해 예외를 다시 던짐
        }
    }
} 