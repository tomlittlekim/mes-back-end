package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.DefectInfo
import kr.co.imoscloud.model.productionmanagement.*
import kr.co.imoscloud.repository.productionmanagement.DefectInfoRepository
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 불량 정보 서비스
 * - 불량 정보 조회, 등록, 수정, 삭제, 통계 기능을 제공
 */
@Service
class DefectInfoService(
    val defectInfoRepository: DefectInfoRepository,
    val workOrderRepository: WorkOrderRepository,
    val productionResultRepository: ProductionResultRepository
) {
    /**
     * 생산 실적 ID로 불량 정보 조회
     */
    fun getDefectInfoByProdResultId(prodResultId: String): List<DefectInfo> {
        val currentUser = getCurrentUserPrincipal()
        return defectInfoRepository.getDefectInfoByProdResultId(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            prodResultId = prodResultId
        )
    }

    /**
     * 다양한 조건으로 불량 정보 목록 조회
     */
    fun getDefectInfoList(filter: DefectInfoFilter): List<DefectInfo> {
        val currentUser = getCurrentUserPrincipal()

        // 날짜 범위 변환
        val fromDateTime = filter.fromDate?.let {
            LocalDateTime.of(LocalDate.parse(it), LocalTime.MIN)
        }
        val toDateTime = filter.toDate?.let {
            LocalDateTime.of(LocalDate.parse(it), LocalTime.MAX)
        }

        // 업데이트된 리포지토리 메소드 호출
        return defectInfoRepository.getDefectInfoList(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            workOrderId = filter.workOrderId,
            prodResultId = filter.prodResultId,
            defectId = filter.defectId,
            productId = filter.productId,
            state = filter.state,
            defectType = filter.defectType,
            equipmentId = null, // 설비 ID 필터링은 비활성화
            fromDate = fromDateTime,
            toDate = toDateTime,
            flagActive = filter.flagActive ?: true
        )
    }

    /**
     * 불량 정보 저장(등록 또는 수정)
     */
    fun saveDefectInfo(
        createdRows: List<DefectInfoInput>? = null,
        updatedRows: List<DefectInfoUpdate>? = null
    ): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()

            // 새로운 불량 정보 등록
            createdRows?.forEach { input ->
                val newDefectInfo = DefectInfo().apply {
                    site = currentUser.getSite()
                    compCd = currentUser.compCd
                    workOrderId = input.workOrderId
                    prodResultId = input.prodResultId
                    defectId = "DF" + System.currentTimeMillis() // 임시 ID 생성 방식
                    productId = input.productId
                    defectQty = input.defectQty
                    resultInfo = input.resultInfo ?: input.defectType
                    state = input.state ?: "NEW" // 기본 상태
                    defectCause = input.defectCause ?: input.defectReason
                    flagActive = input.flagActive ?: true
                    createCommonCol(currentUser)
                }

                defectInfoRepository.save(newDefectInfo)
            }

            // 기존 불량 정보 수정
            updatedRows?.forEach { update ->
                val existingDefectInfo = defectInfoRepository.findByDefectId(update.defectId)

                existingDefectInfo?.let { defectInfo ->
                    defectInfo.apply {
                        update.workOrderId?.let { workOrderId = it }
                        update.prodResultId?.let { prodResultId = it }
                        update.productId?.let { productId = it }
                        update.defectQty?.let { defectQty = it }
                        update.resultInfo?.let { resultInfo = it }
                        update.state?.let { state = it }
                        update.defectCause?.let { defectCause = it }
                        update.flagActive?.let { flagActive = it }
                        updateCommonCol(currentUser)
                    }

                    defectInfoRepository.save(defectInfo)
                }
            }

            return true
        } catch (e: Exception) {
            // 로깅 추가
            println("Error in saveDefectInfo: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * 불량 정보 삭제
     */
    fun deleteDefectInfo(defectId: String): Boolean {
        try {
            val existingDefectInfo = defectInfoRepository.findByDefectId(defectId)

            existingDefectInfo?.let {
                defectInfoRepository.delete(it)
                return true
            }

            return false
        } catch (e: Exception) {
            // 로깅 추가
            println("Error in deleteDefectInfo: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * 생산 실적 등록 시 불량 정보 일괄 등록
     * ProductionResultService에서 호출하는 메소드
     */
    fun saveDefectInfoForProductionResult(
        prodResultId: String,
        workOrderId: String,
        defectInputs: List<DefectInfoInput>
    ): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()

            defectInputs.forEach { input ->
                val newDefectInfo = DefectInfo().apply {
                    site = currentUser.getSite()
                    compCd = currentUser.compCd
                    this.workOrderId = workOrderId
                    this.prodResultId = prodResultId
                    defectId = "DF" + System.currentTimeMillis() + "-" + (input.productId ?: "") // 임시 ID 생성 방식
                    productId = input.productId
                    defectQty = input.defectQty
                    resultInfo = input.resultInfo ?: input.defectType
                    state = input.state ?: "NEW" // 기본 상태
                    defectCause = input.defectCause ?: input.defectReason
                    flagActive = true
                    createCommonCol(currentUser)
                }

                defectInfoRepository.save(newDefectInfo)
            }

            return true
        } catch (e: Exception) {
            // 로깅 추가
            println("Error in saveDefectInfoForProductionResult: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * 제품별 불량 통계 조회
     */
    fun getDefectStatsByProduct(fromDate: LocalDate, toDate: LocalDate): List<DefectStatsByProductDto> {
        val currentUser = getCurrentUserPrincipal()

        // 날짜 범위 변환
        val fromDateTime = LocalDateTime.of(fromDate, LocalTime.MIN)
        val toDateTime = LocalDateTime.of(toDate, LocalTime.MAX)

        // 불량 정보 조회 - 최적화된 메소드 사용
        val defectList = defectInfoRepository.getDefectInfoForStats(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            fromDate = fromDateTime,
            toDate = toDateTime
        )

        // 제품별 통계 계산
        val statsByProduct = mutableMapOf<String, MutableMap<String, Any>>()

        defectList.forEach { defect ->
            val productId = defect.productId ?: "Unknown"

            if (!statsByProduct.containsKey(productId)) {
                statsByProduct[productId] = mutableMapOf(
                    "productId" to productId,
                    "productName" to (getProductName(productId) ?: "Unknown"),
                    "totalDefectQty" to 0.0,
                    "defectCount" to 0,
                    "defectTypes" to mutableMapOf<String, MutableMap<String, Any>>(),
                    "defectCauses" to mutableMapOf<String, MutableMap<String, Any>>()
                )
            }

            val stats = statsByProduct[productId]!!
            stats["totalDefectQty"] = (stats["totalDefectQty"] as Double) + (defect.defectQty ?: 0.0)
            stats["defectCount"] = (stats["defectCount"] as Int) + 1

            // 불량 유형별 집계
            val defectTypes = stats["defectTypes"] as MutableMap<String, MutableMap<String, Any>>
            val defectType = getDefectType(defect) ?: "기타"

            if (!defectTypes.containsKey(defectType)) {
                defectTypes[defectType] = mutableMapOf(
                    "defectType" to defectType,
                    "count" to 0,
                    "qty" to 0.0
                )
            }

            defectTypes[defectType]!!["count"] = (defectTypes[defectType]!!["count"] as Int) + 1
            defectTypes[defectType]!!["qty"] = (defectTypes[defectType]!!["qty"] as Double) + (defect.defectQty ?: 0.0)

            // 불량 원인별 집계
            val defectCauses = stats["defectCauses"] as MutableMap<String, MutableMap<String, Any>>
            val cause = defect.defectCause?.trim()?.takeIf { it.isNotEmpty() } ?: "기타"

            if (!defectCauses.containsKey(cause)) {
                defectCauses[cause] = mutableMapOf(
                    "cause" to cause,
                    "count" to 0,
                    "qty" to 0.0
                )
            }

            defectCauses[cause]!!["count"] = (defectCauses[cause]!!["count"] as Int) + 1
            defectCauses[cause]!!["qty"] = (defectCauses[cause]!!["qty"] as Double) + (defect.defectQty ?: 0.0)
        }

        // 결과를 DTO로 변환
        return statsByProduct.values.map { stats ->
            // 불량 유형별 통계 계산
            val defectTypeStats = (stats["defectTypes"] as Map<String, Map<String, Any>>).values.map { typeStats ->
                val percentage = if (stats["totalDefectQty"] as Double > 0) {
                    ((typeStats["qty"] as Double) / (stats["totalDefectQty"] as Double)) * 100
                } else 0.0

                DefectTypeCountDto(
                    defectType = typeStats["defectType"] as String,
                    count = typeStats["count"] as Int,
                    qty = typeStats["qty"] as Double,
                    percentage = percentage
                )
            }

            // 불량 원인별 통계 계산
            val defectCauseStats = (stats["defectCauses"] as Map<String, Map<String, Any>>).values.map { causeStats ->
                val percentage = if (stats["totalDefectQty"] as Double > 0) {
                    ((causeStats["qty"] as Double) / (stats["totalDefectQty"] as Double)) * 100
                } else 0.0

                DefectCauseCountDto(
                    cause = causeStats["cause"] as String,
                    count = causeStats["count"] as Int,
                    qty = causeStats["qty"] as Double,
                    percentage = percentage
                )
            }

            DefectStatsByProductDto(
                productId = stats["productId"] as String,
                productName = stats["productName"] as String,
                totalDefectQty = stats["totalDefectQty"] as Double,
                defectCount = stats["defectCount"] as Int,
                defectTypes = defectTypeStats,
                defectCauses = defectCauseStats
            )
        }
    }

    /**
     * 원인별 불량 통계 조회
     */
    fun getDefectStatsByCause(fromDate: LocalDate, toDate: LocalDate): List<DefectStatsByCauseDto> {
        val currentUser = getCurrentUserPrincipal()

        // 날짜 범위 변환
        val fromDateTime = LocalDateTime.of(fromDate, LocalTime.MIN)
        val toDateTime = LocalDateTime.of(toDate, LocalTime.MAX)

        // 불량 정보 조회 - 최적화된 메소드 사용
        val defectList = defectInfoRepository.getDefectInfoForStats(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            fromDate = fromDateTime,
            toDate = toDateTime
        )

        // 원인별 통계 계산
        val statsByCause = mutableMapOf<String, MutableMap<String, Any>>()

        defectList.forEach { defect ->
            val cause = defect.defectCause?.trim()?.takeIf { it.isNotEmpty() } ?: "기타"

            if (!statsByCause.containsKey(cause)) {
                statsByCause[cause] = mutableMapOf(
                    "defectCause" to cause,
                    "totalDefectQty" to 0.0,
                    "defectCount" to 0,
                    "products" to mutableMapOf<String, MutableMap<String, Any>>()
                )
            }

            val stats = statsByCause[cause]!!
            stats["totalDefectQty"] = (stats["totalDefectQty"] as Double) + (defect.defectQty ?: 0.0)
            stats["defectCount"] = (stats["defectCount"] as Int) + 1

            // 제품별 집계
            val products = stats["products"] as MutableMap<String, MutableMap<String, Any>>
            val productId = defect.productId ?: "Unknown"
            val productName = getProductName(productId) ?: "Unknown"

            if (!products.containsKey(productId)) {
                products[productId] = mutableMapOf(
                    "productId" to productId,
                    "productName" to productName,
                    "qty" to 0.0,
                    "count" to 0
                )
            }

            products[productId]!!["qty"] = (products[productId]!!["qty"] as Double) + (defect.defectQty ?: 0.0)
            products[productId]!!["count"] = (products[productId]!!["count"] as Int) + 1
        }

        // 결과를 DTO로 변환
        return statsByCause.values.map { stats ->
            // 제품별 불량 통계 계산
            val totalQty = stats["totalDefectQty"] as Double
            val productStats = (stats["products"] as Map<String, Map<String, Any>>).values.map { productStat ->
                val percentage = if (totalQty > 0) {
                    ((productStat["qty"] as Double) / totalQty) * 100
                } else 0.0

                ProductDefectCountDto(
                    productId = productStat["productId"] as String,
                    productName = productStat["productName"] as String,
                    qty = productStat["qty"] as Double,
                    count = productStat["count"] as Int,
                    percentage = percentage
                )
            }

            DefectStatsByCauseDto(
                defectCause = stats["defectCause"] as String,
                totalDefectQty = stats["totalDefectQty"] as Double,
                defectCount = stats["defectCount"] as Int,
                products = productStats
            )
        }
    }

    // 유틸리티 메소드

    /**
     * 제품ID로 제품명 조회
     */
    private fun getProductName(productId: String): String? {
        // 실제 구현에서는 ProductRepository를 통해 조회
        return "제품 $productId"
    }

    /**
     * 불량 유형 추출
     */
    private fun getDefectType(defect: DefectInfo): String? {
        val info = defect.resultInfo ?: return null

        // 불량 유형 분류 로직 (실제 구현에서는 더 정교한 로직으로 대체)
        return when {
            info.contains("외관") -> "외관불량"
            info.contains("기능") -> "기능불량"
            info.contains("치수") -> "치수불량"
            info.contains("재질") -> "재질불량"
            else -> "기타"
        }
    }
}