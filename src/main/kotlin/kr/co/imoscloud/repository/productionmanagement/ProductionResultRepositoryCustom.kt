package kr.co.imoscloud.repository.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionResult

interface ProductionResultRepositoryCustom {
    fun getProductionResultList(
        site: String,
        compCd: String,
        workOrderId: String?,
        prodResultId: String?,
        equipmentId: String?,
        flagActive: Boolean?
    ): List<ProductionResult>

    fun getProductionResultsByWorkOrderId(
        site: String,
        compCd: String,
        workOrderId: String
    ): List<ProductionResult>
}