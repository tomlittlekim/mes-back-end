package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.constants.CoreEnum
import kr.co.imoscloud.entity.material.MaterialMaster
import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.exception.auth.UserNotFoundException
import kr.co.imoscloud.exception.productionmanagement.ProductionPlanSaveFailedException
import kr.co.imoscloud.model.productionmanagement.*
import kr.co.imoscloud.repository.material.MaterialRepository
import kr.co.imoscloud.repository.productionmanagement.ProductionPlanRepository
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipalOrNull
import kr.co.imoscloud.util.DateUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 생산계획 CRUD 서비스
 * 생산계획의 생성, 조회, 수정, 삭제 등 기본적인 데이터 관리를 담당
 */
@Service
class ProductionPlanService(
    val productionPlanRepository: ProductionPlanRepository,
    val materialMasterRepository: MaterialRepository,
    val workOrderRepository: WorkOrderRepository,
) {
    private val log = LoggerFactory.getLogger(ProductionPlanService::class.java)

    // 제품 정보 조회 메서드 (유지)
    fun getProductMaterials(): List<MaterialMaster?> {
        // 사용자 정보 획득
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw UserNotFoundException()

        // 제품 조회
        return materialMasterRepository.findBySiteAndCompCdAndMaterialTypeInAndFlagActiveOrderByMaterialNameAsc(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            materialTypes = listOf(
                CoreEnum.MaterialType.HALF_PRODUCT.key,
                CoreEnum.MaterialType.COMPLETE_PRODUCT.key
            ),
            flagActive = true
        )
    }

    // DTO를 직접 반환하는 생산계획 조회 메서드
    fun getProductionPlans(filter: ProductionPlanFilter): List<ProductionPlanDTO> {
        // 사용자 정보 획득
        val currentUser = getCurrentUserPrincipalOrNull()
            ?: throw UserNotFoundException()

        // DateUtils를 활용하여 String 날짜를 LocalDate로 변환 (기존 변환 로직 유지)
        val planStartDateFrom = DateUtils.parseDate(filter.planStartDateFrom)
        val planStartDateTo = DateUtils.parseDate(filter.planStartDateTo)
        val planEndDateFrom = DateUtils.parseDate(filter.planEndDateFrom)
        val planEndDateTo = DateUtils.parseDate(filter.planEndDateTo)

        // 생산계획 목록 조회 (DTO로 반환)
        return productionPlanRepository.getProductionPlanList(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            prodPlanId = filter.prodPlanId,
            orderId = filter.orderId,
            orderDetailId = filter.orderDetailId,
            productId = filter.productId,
            productName = filter.productName,
            materialCategory = filter.materialCategory,
            shiftType = filter.shiftType,
            planStartDateFrom = planStartDateFrom,
            planStartDateTo = planStartDateTo,
            planEndDateFrom = planEndDateFrom,
            planEndDateTo = planEndDateTo,
            flagActive = filter.flagActive ?: true // 기본값 true 설정
        )
    }

    fun saveProductionPlan(
        createdRows: List<ProductionPlanInput>? = null,
        updatedRows: List<ProductionPlanUpdate>? = null,
    ): Boolean {
        try {
            // 1. 사용자 정보 획득 - 파라미터로 받은 정보가 우선, 없으면 SecurityContext에서 조회
            val currentUser = getCurrentUserPrincipalOrNull()
                ?: throw UserNotFoundException()

            // 생성 요청 처리
            createdRows?.forEach { input ->
                val (startDate, endDate) = input.toLocalDateTimes()

                // 신규 계획 생성
                val newPlan = ProductionPlan().apply {
                    site = currentUser.getSite()
                    compCd = currentUser.compCd
                    prodPlanId = "PP" + System.currentTimeMillis()
                    orderId = input.orderId
                    orderDetailId = input.orderDetailId
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
                        update.orderDetailId?.let { orderDetailId = it }
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
            throw ProductionPlanSaveFailedException()
        }
    }

    /**
     * 생산계획을 소프트 삭제하는 메서드 (flagActive = false로 설정)
     * 활성화된 작업지시가 있는 생산계획은 삭제할 수 없음
     */
    fun softDeleteProductionPlans(prodPlanIds: List<String>): ProductionPlanDeleteResult {
        try {
            // 사용자 정보 획득
            val currentUser = getCurrentUserPrincipalOrNull()
                ?: throw UserNotFoundException()

            var deletedCount = 0
            var skippedCount = 0
            val skippedPlans = mutableListOf<String>()

            prodPlanIds.forEach { prodPlanId ->
                // UK 필드를 활용하여 정확한 레코드 조회
                val existingPlan = productionPlanRepository.findBySiteAndCompCdAndProdPlanId(
                    currentUser.getSite(),
                    currentUser.compCd,
                    prodPlanId
                )

                existingPlan?.let {
                    // 활성화된 작업지시가 있는지 확인
                    val hasActiveWorkOrders = workOrderRepository.existsBySiteAndCompCdAndProdPlanIdAndFlagActive(
                        currentUser.getSite(),
                        currentUser.compCd,
                        prodPlanId,
                        true // flagActive = true인 작업지시 확인
                    )

                    if (hasActiveWorkOrders) {
                        // 활성화된 작업지시가 있으면 삭제하지 않음
                        skippedCount++
                        skippedPlans.add(prodPlanId)
                        log.warn("활성화된 작업지시가 있어 삭제할 수 없는 생산계획: {}", prodPlanId)
                    } else {
                        // 활성화된 작업지시가 없으면 삭제 진행
                        it.softDelete(currentUser)
                        productionPlanRepository.save(it)
                        deletedCount++
                    }
                } ?: log.warn("삭제(비활성화)할 생산계획 없음: {}", prodPlanId)
            }

            if (skippedCount > 0) {
                log.warn("활성화된 작업지시로 인해 삭제되지 않은 생산계획: {} ({}개)", skippedPlans, skippedCount)
            }

            log.info("생산계획 다중 삭제 완료: 요청 {}, 처리 {}, 건너뜀 {}", prodPlanIds.size, deletedCount, skippedCount)
            
            val message = when {
                deletedCount == prodPlanIds.size -> "모든 생산계획이 성공적으로 삭제되었습니다."
                deletedCount > 0 && skippedCount > 0 -> "${deletedCount}개 삭제 완료, ${skippedCount}개는 활성화된 작업지시로 인해 삭제되지 않았습니다."
                deletedCount == 0 && skippedCount > 0 -> "활성화된 작업지시로 인해 삭제할 수 없는 생산계획입니다."
                else -> "삭제할 수 있는 생산계획이 없습니다."
            }

            return ProductionPlanDeleteResult(
                success = deletedCount > 0 || (deletedCount + skippedCount) == prodPlanIds.size,
                totalRequested = prodPlanIds.size,
                deletedCount = deletedCount,
                skippedCount = skippedCount,
                skippedPlans = skippedPlans,
                message = message
            )
        } catch (e: Exception) {
            log.error("생산계획 소프트 삭제 중 오류 발생", e)
            throw ProductionPlanSaveFailedException()
        }
    }
}