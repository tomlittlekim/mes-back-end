package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.entity.material.MaterialMaster
import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.model.productionmanagement.ProductionPlanDTO
import kr.co.imoscloud.model.productionmanagement.ProductionPlanFilter
import kr.co.imoscloud.model.productionmanagement.ProductionPlanInput
import kr.co.imoscloud.model.productionmanagement.ProductionPlanUpdate
import kr.co.imoscloud.repository.material.MaterialRepository
import kr.co.imoscloud.repository.productionmanagement.ProductionPlanRepository
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipalOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ProductionPlanService(
    val productionPlanRepository: ProductionPlanRepository,
    val materialMasterRepository: MaterialRepository
) {
    private val log = LoggerFactory.getLogger(ProductionPlanService::class.java)

    // 제품 정보 조회 메서드 (유지)
    fun getProductMaterials(): List<MaterialMaster?> {
        // 사용자 정보 획득
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        // 제품 조회
        return materialMasterRepository.findBySiteAndCompCdAndFlagActiveOrderByMaterialNameAsc(
            currentUser.getSite(),
            currentUser.compCd,
            true      // 활성화된 데이터만
        )
    }

    // DTO를 직접 반환하는 생산계획 조회 메서드
    fun getProductionPlans(filter: ProductionPlanFilter): List<ProductionPlanDTO> {
        // 사용자 정보 획득
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

        // flagActive가 null인 경우 true로 설정하여 활성화된 데이터만 조회
        val activeFilter = filter.copy(flagActive = filter.flagActive ?: true)

        // 생산계획 목록 조회 (DTO로 반환)
        return productionPlanRepository.getProductionPlanList(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            prodPlanId = activeFilter.prodPlanId,
            orderId = activeFilter.orderId,
            productId = activeFilter.productId,
            productName = activeFilter.productName,
            materialCategory = activeFilter.materialCategory,
            shiftType = activeFilter.shiftType,
            planStartDateFrom = activeFilter.planStartDateFrom,
            planStartDateTo = activeFilter.planStartDateTo,
            planEndDateFrom = activeFilter.planEndDateFrom,
            planEndDateTo = activeFilter.planEndDateTo,
            flagActive = activeFilter.flagActive
        )
    }

    fun saveProductionPlan(
        createdRows: List<ProductionPlanInput>? = null,
        updatedRows: List<ProductionPlanUpdate>? = null,
    ): Boolean {
        try {
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
                    shiftType = input.shiftType
                    planQty = input.planQty
                    planStartDate = startDate
                    planEndDate = endDate
                    flagActive = true // 항상 활성 상태로 생성

                    createCommonCol(currentUser)
                }

                // 저장
                productionPlanRepository.save(newPlan)
            }

            // 수정 요청 처리
            updatedRows?.forEach { update ->
                // UK 필드를 활용하여 정확한 레코드 조회
                val existingPlan = productionPlanRepository.findBySiteAndCompCdAndProdPlanId(
                    currentUser.getSite(),
                    currentUser.compCd,
                    update.prodPlanId
                )

                existingPlan?.let { plan ->
                    val (startDate, endDate) = update.toLocalDateTimes()

                    // 기존 계획 업데이트
                    plan.apply {
                        update.orderId?.let { orderId = it }
                        update.productId?.let { productId = it }
                        update.shiftType?.let { shiftType = it }
                        update.planQty?.let { planQty = it }
                        startDate?.let { planStartDate = it }
                        endDate?.let { planEndDate = it }
                        // flagActive는 업데이트하지 않음 (사용자가 수정할 수 없음)

                        updateCommonCol(currentUser)
                    }

                    // 저장
                    productionPlanRepository.save(plan)
                } ?: log.warn("업데이트할 생산계획을 찾을 수 없습니다: {}", update.prodPlanId)
            }

            return true
        } catch (e: Exception) {
            log.error("생산계획 저장 중 오류 발생", e)
            throw e  // 오류를 상위로 전파하도록 변경
        }
    }

    /**
     * 생산계획을 소프트 삭제하는 메서드 (flagActive = false로 설정)
     */
    fun softDeleteProductionPlan(prodPlanId: String): Boolean {
        try {
            // 사용자 정보 획득
            val currentUser = getCurrentUserPrincipalOrNull()
                ?: throw SecurityException("사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다.")

            // UK 필드를 활용하여 정확한 레코드 조회
            val existingPlan = productionPlanRepository.findBySiteAndCompCdAndProdPlanId(
                currentUser.getSite(),
                currentUser.compCd,
                prodPlanId
            )

            existingPlan?.let {
                // flagActive를 false로 설정
                it.flagActive = false
                it.updateCommonCol(currentUser)

                productionPlanRepository.save(it)
                return true
            }

            log.warn("삭제(비활성화)할 생산계획 없음: {}", prodPlanId)
            return false
        } catch (e: Exception) {
            log.error("생산계획 소프트 삭제 중 오류 발생", e)
            throw e  // 오류를 상위로 전파하도록 변경
        }
    }
}