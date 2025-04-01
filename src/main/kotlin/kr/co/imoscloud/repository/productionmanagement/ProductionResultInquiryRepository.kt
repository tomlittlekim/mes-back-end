package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 생산실적조회 전용 리포지토리
 * - 조회 중심의 기능으로 별도 분리
 */
interface ProductionResultInquiryRepository : JpaRepository<ProductionResult, Long>, ProductionResultInquiryRepositoryCustom {
    // 기본 JPA 메소드는 그대로 사용
    fun findByProdResultId(prodResultId: String): ProductionResult?
}