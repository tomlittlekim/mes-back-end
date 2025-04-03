package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import java.time.LocalDateTime

/**
 * 생산실적조회 확장 리포지토리 인터페이스
 * - 조회 중심의 확장 쿼리 메소드 정의
 */
interface ProductionResultInquiryRepositoryCustom {
    /**
     * 다양한 조건으로 생산실적 목록 조회
     * - 기존 getProductionResultList 기능 확장
     */
    fun getProductionResultList(
        site: String,
        compCd: String,
        workOrderId: String? = null,
        prodResultId: String? = null,
        productId: String? = null,
        equipmentId: String? = null,
        fromDate: LocalDateTime? = null,
        toDate: LocalDateTime? = null,
        status: String? = null,
        flagActive: Boolean = true
    ): List<ProductionResult>

    /**
     * 특정 생산실적 ID로 상세 정보 조회
     */
    fun findDetailByProdResultId(
        site: String,
        compCd: String,
        prodResultId: String
    ): ProductionResult?

    /**
     * 기간별 생산실적 목록 조회
     * - 통계 계산용
     */
    fun getProductionResultListByDateRange(
        site: String,
        compCd: String,
        fromDate: LocalDateTime,
        toDate: LocalDateTime
    ): List<ProductionResult>

    /**
     * 설비별 생산실적 집계 조회
     */
    fun getProductionResultByEquipment(
        site: String,
        compCd: String,
        fromDate: LocalDateTime,
        toDate: LocalDateTime
    ): List<ProductionResult>

    /**
     * 제품별 생산실적 집계 조회
     */
    fun getProductionResultByProduct(
        site: String,
        compCd: String,
        fromDate: LocalDateTime,
        toDate: LocalDateTime
    ): List<ProductionResult>
}