package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionPlan
import org.springframework.data.jpa.repository.JpaRepository

interface ProductionPlanRepository: JpaRepository<ProductionPlan, Long>, ProductionPlanRepositoryCustom {
    fun findByProdPlanId(prodPlanId: String): ProductionPlan?

    // UK 필드를 모두 사용하는 메서드 추가
    fun findBySiteAndCompCdAndProdPlanId(site: String, compCd: String, prodPlanId: String): ProductionPlan?
}