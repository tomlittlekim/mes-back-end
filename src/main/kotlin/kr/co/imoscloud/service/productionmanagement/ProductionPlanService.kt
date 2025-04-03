package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.model.productionmanagement.ProductionPlanFilter
import kr.co.imoscloud.model.productionmanagement.ProductionPlanInput
import kr.co.imoscloud.model.productionmanagement.ProductionPlanUpdate
import kr.co.imoscloud.repository.productionmanagement.ProductionPlanRepository
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipalOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ProductionPlanService(
    val productionPlanRepository: ProductionPlanRepository
) {
    private val log = LoggerFactory.getLogger(ProductionPlanService::class.java)

    fun getProductionPlans(filter: ProductionPlanFilter): List<ProductionPlan> {
        // 1. 파라미터로 받은 사용자 정보가 있으면 사용, 없으면 SecurityUtils에서 가져오기 시도
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        return productionPlanRepository.getProductionPlanList(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            prodPlanId = filter.prodPlanId,
            orderId = filter.orderId,
            productId = filter.productId,
            planStartDate = filter.planStartDate,
            planEndDate = filter.planEndDate,
            flagActive = filter.flagActive
        )
    }

    fun saveProductionPlan(
        createdRows: List<ProductionPlanInput>? = null,
        updatedRows: List<ProductionPlanUpdate>? = null,
    ): Boolean {
        try {
            // 인증 정보 확인 및 로깅
            log.debug("서비스에서 인증 정보 요청 처리 시작")

            // 1. 사용자 정보 획득 - 파라미터로 받은 정보가 우선, 없으면 SecurityContext에서 조회
            val currentUser = getCurrentUserPrincipalOrNull()
                ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

            // 생성 요청 처리
            createdRows?.forEach { input ->
                val (startDate, endDate) = input.toLocalDateTimes()

                // 신규 계획 생성
                val newPlan = ProductionPlan().apply {
                    site = currentUser.getSite()
                    compCd = currentUser.compCd
                    prodPlanId = "PP" + System.currentTimeMillis()
                    orderId = input.orderId
                    productId = input.productId
                    planQty = input.planQty
                    planStartDate = startDate
                    planEndDate = endDate
                    flagActive = input.flagActive ?: true

                    createCommonCol(currentUser)
                }

                // 저장
                productionPlanRepository.save(newPlan)
                log.debug("새 생산계획 저장: {}", newPlan.prodPlanId)
            }

            // 수정 요청 처리
            updatedRows?.forEach { update ->
                val existingPlan = productionPlanRepository.findByProdPlanId(update.prodPlanId)

                existingPlan?.let { plan ->
                    val (startDate, endDate) = update.toLocalDateTimes()

                    // 기존 계획 업데이트
                    plan.apply {
                        update.orderId?.let { orderId = it }
                        update.productId?.let { productId = it }
                        update.planQty?.let { planQty = it }
                        startDate?.let { planStartDate = it }
                        endDate?.let { planEndDate = it }
                        update.flagActive?.let { flagActive = it }

                        updateCommonCol(currentUser)
                    }

                    // 저장
                    productionPlanRepository.save(plan)
                    log.debug("생산계획 업데이트: {}", plan.prodPlanId)
                }
            }

            return true
        } catch (e: Exception) {
            log.error("생산계획 저장 중 오류 발생", e)
            throw e  // 오류를 상위로 전파하도록 변경
        }
    }
    fun deleteProductionPlan(prodPlanId: String): Boolean {
        try {
            // 사용자 정보 획득
            val currentUser = getCurrentUserPrincipalOrNull()
                ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

            val existingPlan = productionPlanRepository.findByProdPlanId(prodPlanId)

            existingPlan?.let {
                productionPlanRepository.delete(it)
                log.debug("생산계획 삭제: {}", prodPlanId)
                return true
            }

            log.warn("삭제할 생산계획 없음: {}", prodPlanId)
            return false
        } catch (e: Exception) {
            log.error("생산계획 삭제 중 오류 발생", e)
            throw e  // 오류를 상위로 전파하도록 변경
        }
    }}