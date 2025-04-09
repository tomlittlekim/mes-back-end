package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.DefectInfo
import kr.co.imoscloud.model.productionmanagement.*
import kr.co.imoscloud.repository.productionmanagement.DefectInfoRepository
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 불량 정보 서비스
 * - 불량 정보 조회, 등록, 수정, 삭제, 통계 기능을 제공
 */
@Service
class DefectInfoService(
    private val defectInfoRepository: DefectInfoRepository,
    private val workOrderRepository: WorkOrderRepository,
    private val productionResultRepository: ProductionResultRepository
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
     * DefectInfo 엔티티를 DefectInfoDto로 변환하는 유틸리티 메소드
     */
    fun convertToDefectInfoDtos(defectInfos: List<DefectInfo>): List<DefectInfoDto> {
        return defectInfos.map { defect ->
            DefectInfoDto(
                defectId = defect.defectId,
                workOrderId = defect.workOrderId,
                prodResultId = defect.prodResultId,
                productId = defect.productId,
                defectName = defect.resultInfo, // 불량명 정보가 resultInfo에 저장됨
                defectQty = defect.defectQty,
                defectCause = defect.defectCause,
                state = defect.state,
                resultInfo = defect.resultInfo,
                createDate = defect.createDate?.format(DateTimeFormatter.ISO_DATE_TIME),
                updateDate = defect.updateDate?.format(DateTimeFormatter.ISO_DATE_TIME),
                createUser = defect.createUser,
                updateUser = defect.updateUser
            )
        }
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
            equipmentId = null, // 설비 ID 필터링은 선택적으로 활성화
            fromDate = fromDateTime,
            toDate = toDateTime,
            flagActive = filter.flagActive ?: true
        )
    }

    /**
     * 불량 정보 저장(등록 또는 수정)
     */
    @Transactional
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
                    flagActive = true
                    createCommonCol(currentUser)
                }

                defectInfoRepository.save(newDefectInfo)

                // 생산실적 불량 수량 업데이트 (선택적)
                updateProductionResultDefectQty(input.prodResultId)
            }

            // 기존 불량 정보 수정
            updatedRows?.forEach { update ->
                val existingDefectInfo = defectInfoRepository.findByDefectId(update.defectId)

                existingDefectInfo?.let { defectInfo ->
                    // 기존 불량 수량 저장
                    val originalQty = defectInfo.defectQty ?: 0.0

                    defectInfo.apply {
                        update.workOrderId?.let { workOrderId = it }
                        update.prodResultId?.let { prodResultId = it }
                        update.defectName?.let { resultInfo = it }
                        update.productId?.let { productId = it }
                        update.defectQty?.let { defectQty = it }
                        update.resultInfo?.let { resultInfo = it }
                        update.state?.let { state = it }
                        update.defectCause?.let { defectCause = it }
                        update.flagActive?.let { flagActive = it }
                        updateCommonCol(currentUser)
                    }

                    defectInfoRepository.save(defectInfo)

                    // 불량 수량이 변경된 경우 생산실적 불량 수량 업데이트
                    if (originalQty != (update.defectQty ?: originalQty)) {
                        updateProductionResultDefectQty(defectInfo.prodResultId!!)
                    }
                }
            }

            return true
        } catch (e: Exception) {
            println("Error in saveDefectInfo: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * 불량 정보 삭제 (물리적 삭제)
     */
    @Transactional
    fun deleteDefectInfo(defectId: String): Boolean {
        try {
            val existingDefectInfo = defectInfoRepository.findByDefectId(defectId)

            existingDefectInfo?.let {
                val prodResultId = it.prodResultId
                defectInfoRepository.delete(it)

                // 생산실적 불량 수량 업데이트
                if (prodResultId != null) {
                    updateProductionResultDefectQty(prodResultId)
                }

                return true
            }

            return false
        } catch (e: Exception) {
            println("Error in deleteDefectInfo: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * 불량 정보 소프트 삭제 (flagActive = false로 설정)
     */
    @Transactional
    fun softDeleteDefectInfo(defectId: String): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()
            val existingDefectInfo = defectInfoRepository.findByDefectId(defectId)

            existingDefectInfo?.let {
                val prodResultId = it.prodResultId

                // flagActive를 false로 설정
                it.flagActive = false
                it.updateCommonCol(currentUser)
                defectInfoRepository.save(it)

                // 생산실적 불량 수량 업데이트
                if (prodResultId != null) {
                    updateProductionResultDefectQty(prodResultId)
                }

                return true
            }

            return false
        } catch (e: Exception) {
            println("Error in softDeleteDefectInfo: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * 생산 실적 등록 시 불량 정보 일괄 등록
     * ProductionResultService에서 호출하는 메소드
     */
    @Transactional
    fun saveDefectInfoForProductionResult(
        prodResultId: String,
        workOrderId: String,
        defectInputs: List<DefectInfoInput>
    ): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()
            var totalDefectQty = 0.0

            defectInputs.forEach { input ->
                val newDefectInfo = DefectInfo().apply {
                    site = currentUser.getSite()
                    compCd = currentUser.compCd
                    this.workOrderId = workOrderId
                    this.prodResultId = prodResultId
                    defectId = "DF" + System.currentTimeMillis() + "-" + (Math.random() * 1000).toInt() // 고유 ID 생성
                    productId = input.productId
                    defectQty = input.defectQty ?: 0.0
                    resultInfo = input.resultInfo ?: input.defectType ?: input.defectName
                    state = input.state ?: "NEW" // 기본 상태
                    defectCause = input.defectCause ?: input.defectReason
                    flagActive = true
                    createCommonCol(currentUser)
                }

                defectInfoRepository.save(newDefectInfo)
                totalDefectQty += newDefectInfo.defectQty ?: 0.0
            }

            // 생산실적 불량 수량 업데이트 (필요시)
            if (defectInputs.isNotEmpty()) {
                updateProductionResultDefectQty(prodResultId)
            }

            return true
        } catch (e: Exception) {
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

    /**
     * 생산실적의 불량 수량을 업데이트하는 내부 메소드
     * - 불량정보 추가/수정/삭제 시 호출하여 생산실적의 불량 수량을 동기화
     */
    @Transactional
    private fun updateProductionResultDefectQty(prodResultId: String?) {
        if (prodResultId == null) return

        try {
            val currentUser = getCurrentUserPrincipal()

            // 생산실적 조회
            val productionResult = productionResultRepository.findBySiteAndCompCdAndProdResultId(
                currentUser.getSite(),
                currentUser.compCd,
                prodResultId
            ) ?: return

            // 활성화된 불량정보만 조회하여 합계 계산
            val defectInfos = defectInfoRepository.getDefectInfoByProdResultId(
                currentUser.getSite(),
                currentUser.compCd,
                prodResultId
            ).filter { it.flagActive == true }

            // 불량 수량 합산
            val totalDefectQty = defectInfos.sumOf { it.defectQty ?: 0.0 }

            // 생산실적 업데이트
            val goodQty = productionResult.goodQty ?: 0.0
            val totalQty = goodQty + totalDefectQty

            // 작업지시 조회하여 계획수량 확인
            val workOrder = productionResult.workOrderId?.let {
                workOrderRepository.findBySiteAndCompCdAndWorkOrderId(
                    currentUser.getSite(),
                    currentUser.compCd,
                    it
                )
            }

            val orderQty = workOrder?.orderQty ?: 0.0

            // 진척률 및 불량률 재계산
            val progressRate = if (orderQty > 0) {
                String.format("%.1f", (totalQty / orderQty) * 100.0)
            } else "0.0"

            val defectRate = if (totalQty > 0) {
                String.format("%.1f", (totalDefectQty / totalQty) * 100.0)
            } else "0.0"

            // 생산실적 업데이트
            productionResult.apply {
                this.defectQty = totalDefectQty
                this.progressRate = progressRate
                this.defectRate = defectRate
                updateCommonCol(currentUser)
            }

            productionResultRepository.save(productionResult)

        } catch (e: Exception) {
            println("Error in updateProductionResultDefectQty: ${e.message}")
            // 로그만 기록하고 예외는 던지지 않음
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