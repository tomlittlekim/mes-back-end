package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import kr.co.imoscloud.model.productionmanagement.ProductionPlanFilter
import kr.co.imoscloud.model.productionmanagement.ProductionPlanInput
import kr.co.imoscloud.model.productionmanagement.ProductionPlanUpdate
import kr.co.imoscloud.repository.productionmanagement.ProductionPlanRepository
import kr.co.imoscloud.security.UserPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class ProductionPlanService(
    val productionPlanRepository: ProductionPlanRepository
) {
    fun getProductionPlans(filter: ProductionPlanFilter): List<ProductionPlan> {
        return productionPlanRepository.getProductionPlanList(
            site = "imos",
            compCd = "epin",
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
        updatedRows: List<ProductionPlanUpdate>? = null
    ): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()

            // 새로운 계획 저장
            createdRows?.forEach { input ->
                val (startDate, endDate) = input.toLocalDateTimes()

                val newPlan = ProductionPlan().apply {
                    site = currentUser.getSite()
                    compCd = currentUser.getCompCd()
                    prodPlanId = "PP" + System.currentTimeMillis() // 임시 ID 생성 방식
                    orderId = input.orderId
                    productId = input.productId
                    planQty = input.planQty
                    planStartDate = startDate
                    planEndDate = endDate
                    flagActive = input.flagActive ?: true
                    createCommonCol(currentUser)
                }

                productionPlanRepository.save(newPlan)
            }

            // 기존 계획 업데이트
            updatedRows?.forEach { update ->
                val existingPlan = productionPlanRepository.findByProdPlanId(update.prodPlanId)

                existingPlan?.let { plan ->
                    val (startDate, endDate) = update.toLocalDateTimes()

                    plan.apply {
                        update.orderId?.let { orderId = it }
                        update.productId?.let { productId = it }
                        update.planQty?.let { planQty = it }
                        startDate?.let { planStartDate = it }
                        endDate?.let { planEndDate = it }
                        update.flagActive?.let { flagActive = it }
                        updateCommonCol(currentUser)
                    }

                    productionPlanRepository.save(plan)
                }
            }

            return true
        } catch (e: Exception) {
            // 로깅 추가
            println("Error in saveProductionPlan: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun deleteProductionPlan(prodPlanId: String): Boolean {
        try {
            val existingPlan = productionPlanRepository.findByProdPlanId(prodPlanId)

            existingPlan?.let {
                productionPlanRepository.delete(it)
                return true
            }

            return false
        } catch (e: Exception) {
            // 로깅 추가
            println("Error in deleteProductionPlan: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    private fun getCurrentUserPrincipal(): UserPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication != null && authentication.isAuthenticated && authentication.principal is UserPrincipal) {
            return authentication.principal as UserPrincipal
        }

        // 인증 정보가 없거나 UserPrincipal이 아닌 경우 예외 처리
        throw SecurityException("현재 인증된 사용자 정보를 찾을 수 없습니다.")
    }
}