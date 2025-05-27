package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.model.productionmanagement.ProductionResultFilter
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



    /**
     * 다중 생산실적 배치 소프트 삭제 (QueryDSL + @Transactional)
     */
    fun batchSoftDeleteProductionResults(
        site: String,
        compCd: String,
        prodResultIds: List<String>,
        updateUser: String,
        updateDate: java.time.LocalDateTime
    ): Long



}