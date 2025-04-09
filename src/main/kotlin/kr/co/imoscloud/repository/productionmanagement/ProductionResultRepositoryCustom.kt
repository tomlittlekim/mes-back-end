package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import java.time.LocalDate
import java.time.LocalDateTime

interface ProductionResultRepositoryCustom {
    /**
     * 작업지시ID로 생산실적 목록 조회
     */
    fun getProductionResultsByWorkOrderId(
        site: String,
        compCd: String,
        workOrderId: String
    ): List<ProductionResult>

    /**
     * 다양한 조건으로 기본 생산실적 목록 조회
     */
    fun getProductionResultList(
        site: String,
        compCd: String,
        workOrderId: String?,
        prodResultId: String?,
        equipmentId: String?,
        planStartDateFrom: LocalDate?,  // 날짜 필드 타입 변경
        planStartDateTo: LocalDate?,    // 날짜 필드 타입 변경
        flagActive: Boolean?
    ): List<ProductionResult>

    /**
     * 다양한 조건으로 생산실적 목록 조회 (DetailView용)
     * - 상세 조회를 위한 확장 버전
     */
    fun getProductionResultListWithDetails(
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