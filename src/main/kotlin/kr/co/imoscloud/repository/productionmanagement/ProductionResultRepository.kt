package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import org.springframework.data.jpa.repository.JpaRepository

interface ProductionResultRepository : JpaRepository<ProductionResult, Long>, ProductionResultRepositoryCustom {
    fun findBySiteAndCompCdAndProdResultId(site: String, compCd: String, prodResultId: String): ProductionResult?
}