package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.model.productionmanagement.ProductionPlanFilter
import kr.co.imoscloud.model.productionmanagement.ProductionPlanInput
import kr.co.imoscloud.model.productionmanagement.ProductionPlanUpdate
import kr.co.imoscloud.repository.productionmanagement.ProductionPlanRepository
import kr.co.imoscloud.security.UserPrincipal
import kr.co.imoscloud.util.SecurityUtils
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ProductionPlanService(
    val productionPlanRepository: ProductionPlanRepository
) {
    private val log = LoggerFactory.getLogger(ProductionPlanService::class.java)

    // 기본 값 - 환경 설정이나 상수에서 가져오도록 수정 가능
    private val DEFAULT_SITE = "imos"
    private val DEFAULT_COMP_CD = "8pin"
    private val DEFAULT_USER = "system"

    fun getProductionPlans(filter: ProductionPlanFilter, userPrincipal: UserPrincipal? = null): List<ProductionPlan> {
        // 1. 파라미터로 받은 사용자 정보가 있으면 사용, 없으면 SecurityUtils에서 가져오기 시도
        val currentUser = userPrincipal ?: SecurityUtils.getCurrentUserPrincipalOrNull()

        return productionPlanRepository.getProductionPlanList(
            site = currentUser?.getSite() ?: DEFAULT_SITE,
            compCd = currentUser?.getCompCd() ?: DEFAULT_COMP_CD,
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
        userPrincipal: UserPrincipal? = null  // 명시적으로 사용자 정보를 받을 수 있도록 파라미터 추가
    ): Boolean {
        try {
            // 인증 정보 확인 및 로깅
            log.debug("서비스에서 인증 정보 요청 처리 시작")

            // 1. 사용자 정보 획득 - 파라미터로 받은 정보가 우선, 없으면 SecurityContext에서 조회
            val currentUser = userPrincipal ?: try {
                SecurityUtils.getCurrentUserPrincipal().also {
                    log.debug("현재 사용자: {}, 사이트: {}, 회사: {}",
                        it.getUsername(), it.getSite(), it.getCompCd())
                }
            } catch (e: SecurityException) {
                log.warn("인증된 사용자 정보를 찾을 수 없음, 기본 값 사용: {}", e.message)
                null
            }

            // 생성 요청 처리
            createdRows?.forEach { input ->
                val (startDate, endDate) = input.toLocalDateTimes()

                // 신규 계획 생성
                val newPlan = ProductionPlan().apply {
                    site = currentUser?.getSite() ?: DEFAULT_SITE
                    compCd = currentUser?.getCompCd() ?: DEFAULT_COMP_CD
                    prodPlanId = "PP" + System.currentTimeMillis()
                    orderId = input.orderId
                    productId = input.productId
                    planQty = input.planQty
                    planStartDate = startDate
                    planEndDate = endDate
                    flagActive = input.flagActive ?: true

                    // 사용자 정보가 있으면 해당 정보로, 없으면 기본값으로 생성 정보 설정
                    if (currentUser != null) {
                        createUser = currentUser.getUserId()
                    } else {
                        createUser = DEFAULT_USER
                    }
                    createDate = LocalDateTime.now()
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

                        // 사용자 정보가 있으면 해당 정보로, 없으면 기본값으로 업데이트 정보 설정
                        if (currentUser != null) {
                            updateUser = currentUser.getUserId()
                        } else {
                            updateUser = DEFAULT_USER
                        }
                        updateDate = LocalDateTime.now()
                    }

                    // 저장
                    productionPlanRepository.save(plan)
                    log.debug("생산계획 업데이트: {}", plan.prodPlanId)
                }
            }

            return true
        } catch (e: Exception) {
            log.error("생산계획 저장 중 오류 발생", e)
            return false
        }
    }

    fun deleteProductionPlan(prodPlanId: String, userPrincipal: UserPrincipal? = null): Boolean {
        try {
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
            return false
        }
    }
}