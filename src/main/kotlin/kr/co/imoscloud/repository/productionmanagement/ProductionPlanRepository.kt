package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import org.springframework.data.jpa.repository.JpaRepository

interface ProductionPlanRepository: JpaRepository<ProductionPlan, Long>, ProductionPlanRepositoryCustom {
    fun findByProdPlanId(prodPlanId: String): ProductionPlan?
}