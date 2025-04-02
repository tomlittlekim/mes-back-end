package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.model.productionmanagement.ProductionResultFilter
import kr.co.imoscloud.model.productionmanagement.ProductionResultInput
import kr.co.imoscloud.model.productionmanagement.ProductionResultUpdate
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import org.springframework.stereotype.Service

@Service
class ProductionResultService(
    val productionResultRepository: ProductionResultRepository
) {
    fun getProductionResultsByWorkOrderId(workOrderId: String): List<ProductionResult> {
        val currentUser = getCurrentUserPrincipal()
        return productionResultRepository.getProductionResultsByWorkOrderId(
            site = currentUser.getSite(),
            compCd = currentUser.getCompCd(),
            workOrderId = workOrderId
        )
    }

    fun getProductionResults(filter: ProductionResultFilter): List<ProductionResult> {
        return productionResultRepository.getProductionResultList(
            site = "imos",
            compCd = "8pin",
            workOrderId = filter.workOrderId,
            prodResultId = filter.prodResultId,
            equipmentId = filter.equipmentId,
            flagActive = filter.flagActive
        )
    }

    fun saveProductionResult(
        createdRows: List<ProductionResultInput>? = null,
        updatedRows: List<ProductionResultUpdate>? = null
    ): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()

            // 새로운 생산실적 저장
            createdRows?.forEach { input ->
                // 양품과 불량품 수량으로부터 수율 계산
                val goodQty = input.goodQty ?: 0.0
                val defectQty = input.defectQty ?: 0.0
                val totalQty = goodQty + defectQty

                val progressRate = if (totalQty > 0) {
                    String.format("%.1f", (goodQty / totalQty) * 100.0)
                } else "0.0"

                val defectRate = if (totalQty > 0) {
                    String.format("%.1f", (defectQty / totalQty) * 100.0)
                } else "0.0"

                val newResult = ProductionResult().apply {
                    site = currentUser.getSite()
                    compCd = currentUser.getCompCd()
                    prodResultId = "PR" + System.currentTimeMillis() // 임시 ID 생성 방식
                    workOrderId = input.workOrderId
                    this.goodQty = goodQty
                    this.defectQty = defectQty
                    this.progressRate = progressRate
                    this.defectRate = defectRate
                    equipmentId = input.equipmentId
                    resultInfo = input.resultInfo
                    defectCause = input.defectCause
                    flagActive = input.flagActive ?: true
                    createCommonCol(currentUser)
                }

                productionResultRepository.save(newResult)
            }

            // 기존 생산실적 업데이트
            updatedRows?.forEach { update ->
                val existingResult = productionResultRepository.findByProdResultId(update.prodResultId)

                existingResult?.let { result ->
                    // 업데이트된 양품과 불량품 수량을 가져옴
                    val goodQty = update.goodQty ?: result.goodQty ?: 0.0
                    val defectQty = update.defectQty ?: result.defectQty ?: 0.0
                    val totalQty = goodQty + defectQty

                    val progressRate = if (totalQty > 0) {
                        String.format("%.1f", (goodQty / totalQty) * 100.0)
                    } else "0.0"

                    val defectRate = if (totalQty > 0) {
                        String.format("%.1f", (defectQty / totalQty) * 100.0)
                    } else "0.0"

                    result.apply {
                        update.workOrderId?.let { workOrderId = it }
                        update.goodQty?.let { this.goodQty = it }
                        update.defectQty?.let { this.defectQty = it }
                        this.progressRate = progressRate
                        this.defectRate = defectRate
                        update.equipmentId?.let { equipmentId = it }
                        update.resultInfo?.let { resultInfo = it }
                        update.defectCause?.let { defectCause = it }
                        update.flagActive?.let { flagActive = it }
                        updateCommonCol(currentUser)
                    }

                    productionResultRepository.save(result)
                }
            }

            return true
        } catch (e: Exception) {
            // 로깅 추가
            println("Error in saveProductionResult: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun deleteProductionResult(prodResultId: String): Boolean {
        try {
            val existingResult = productionResultRepository.findByProdResultId(prodResultId)

            existingResult?.let {
                productionResultRepository.delete(it)
                return true
            }

            return false
        } catch (e: Exception) {
            // 로깅 추가
            println("Error in deleteProductionResult: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

}