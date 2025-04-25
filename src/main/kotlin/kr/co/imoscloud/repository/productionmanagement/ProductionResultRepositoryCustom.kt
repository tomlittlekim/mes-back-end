package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.model.productionmanagement.ProductionResultFilter
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
    fun getProductionResults(
        site: String,
        compCd: String,
        workOrderId: String?,
        prodResultId: String?,
        productId: String?,
        equipmentId: String?,
        warehouseId: String?,
        prodStartTimeFrom: LocalDateTime?,
        prodStartTimeTo: LocalDateTime?,
        prodEndTimeFrom: LocalDateTime?,
        prodEndTimeTo: LocalDateTime?,
        flagActive: Boolean?
    ): List<ProductionResult>

    fun getProductionResultsAtMobile(site: String, compCd: String, filter: ProductionResultFilter?): List<ProductionResult>

}