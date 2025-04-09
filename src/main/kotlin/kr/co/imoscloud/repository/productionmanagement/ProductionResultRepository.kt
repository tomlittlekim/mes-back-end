package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import org.springframework.data.jpa.repository.JpaRepository

interface ProductionResultRepository : JpaRepository<ProductionResult, Long>, ProductionResultRepositoryCustom {
    fun findByProdResultId(prodResultId: String): ProductionResult?

    // Unique Key 필드를 모두 사용하는 메서드 추가
    fun findBySiteAndCompCdAndProdResultId(site: String, compCd: String, prodResultId: String): ProductionResult?
}