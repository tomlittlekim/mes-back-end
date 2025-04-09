package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.model.productionmanagement.*
import kr.co.imoscloud.repository.productionmanagement.DefectInfoRepository
import kr.co.imoscloud.repository.productionmanagement.ProductionResultRepository
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
class ProductionResultService(
    private val productionResultRepository: ProductionResultRepository,
    private val workOrderRepository: WorkOrderRepository,
    private val defectInfoService: DefectInfoService,
    private val defectInfoRepository: DefectInfoRepository
) {
    private val log = LoggerFactory.getLogger(ProductionResultService::class.java)

    /**
     * 작업지시ID로 생산실적 목록 조회
     */
    fun getProductionResultsByWorkOrderId(workOrderId: String): List<ProductionResult> {
        val currentUser = getCurrentUserPrincipal()
        // 활성화된 생산실적만 조회 (기본적으로 Repository 함수에 flagActive=true 조건이 있음)
        return productionResultRepository.getProductionResultsByWorkOrderId(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            workOrderId = workOrderId
        )
    }

    /**
     * 다양한 필터 조건으로 생산실적 목록 조회
     */
    fun getProductionResults(filter: ProductionResultFilter): List<ProductionResult> {
        val currentUser = getCurrentUserPrincipal()
        // flagActive가 명시적으로 설정되지 않은 경우 true로 설정하여 활성화된 데이터만 조회
        val activeFilter = filter.copy(flagActive = filter.flagActive ?: true)

        return productionResultRepository.getProductionResultList(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            workOrderId = activeFilter.workOrderId,
            prodResultId = activeFilter.prodResultId,
            equipmentId = activeFilter.equipmentId,
            planStartDateFrom = activeFilter.planStartDateFrom,
            planStartDateTo = activeFilter.planStartDateTo,
            flagActive = activeFilter.flagActive
        )
    }

    /**
     * 생산실적 요약 목록 조회
     */
    fun getProductionResultSummaryList(filter: ProductionResultInquiryFilter): List<ProductionResultSummaryDto> {
        val currentUser = getCurrentUserPrincipal()

        // 문자열 날짜를 LocalDateTime으로 변환
        val fromDateTime = filter.fromDate?.let {
            LocalDateTime.of(LocalDate.parse(it), LocalTime.MIN)
        }

        val toDateTime = filter.toDate?.let {
            LocalDateTime.of(LocalDate.parse(it), LocalTime.MAX)
        }

        val results = productionResultRepository.getProductionResultListWithDetails(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            workOrderId = filter.workOrderId,
            prodResultId = filter.prodResultId,
            productId = filter.productId,
            equipmentId = filter.equipmentId,
            fromDate = fromDateTime,
            toDate = toDateTime,
            status = filter.status,
            flagActive = filter.flagActive ?: true
        )

        // 생산실적 결과를 요약 DTO로 변환
        return results.map { result ->
            // 관련 작업지시 정보 조회
            val workOrder = result.workOrderId?.let {
                workOrderRepository.findByWorkOrderId(it)
            }

            // 제품명 가져오기
            val productName = workOrder?.productId?.let { getProductName(it) }

            ProductionResultSummaryDto(
                id = result.id,
                prodResultId = result.prodResultId,
                workOrderId = result.workOrderId,
                productId = workOrder?.productId,
                productName = productName,
                equipmentId = result.equipmentId,
                equipmentName = result.equipmentId?.let { getEquipmentName(it) },
                productionDate = result.createDate?.format(DateTimeFormatter.ISO_DATE),
                planQuantity = workOrder?.orderQty,
                actualQuantity = result.goodQty,
                defectQuantity = result.defectQty,
                progressRate = result.progressRate,
                defectRate = result.defectRate,
                worker = result.createUser,
                status = workOrder?.state,
                createDate = result.createDate?.format(DateTimeFormatter.ISO_DATE_TIME),
                updateDate = result.updateDate?.format(DateTimeFormatter.ISO_DATE_TIME)
            )
        }
    }

    /**
     * 생산실적 상세 조회
     */
    fun getProductionResultDetail(prodResultId: String): ProductionResultDetailDto? {
        val currentUser = getCurrentUserPrincipal()
        val result = productionResultRepository.findBySiteAndCompCdAndProdResultId(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            prodResultId = prodResultId
        ) ?: return null

        val workOrder = result.workOrderId?.let {
            workOrderRepository.findByWorkOrderId(it)
        }

        val productionDate = result.createDate?.toLocalDate() ?: LocalDate.now()

        // 제품명 조회
        val productName = workOrder?.productId?.let { getProductName(it) }

        return ProductionResultDetailDto(
            id = result.id,
            prodResultId = result.prodResultId,
            workOrderId = result.workOrderId,
            productId = workOrder?.productId,
            productName = productName,
            factoryId = result.equipmentId?.let { getFactoryId(it) },
            factoryName = result.equipmentId?.let { getFactoryName(it) },
            lineId = result.equipmentId?.let { getLineId(it) },
            lineName = result.equipmentId?.let { getLineName(it) },
            equipmentId = result.equipmentId,
            equipmentName = result.equipmentId?.let { getEquipmentName(it) },
            productionDate = productionDate.format(DateTimeFormatter.ISO_DATE),
            planQuantity = workOrder?.orderQty,
            goodQuantity = result.goodQty,
            defectQuantity = result.defectQty,
            inputAmount = calculateInputAmount(result),
            outputAmount = calculateOutputAmount(result),
            yieldRate = calculateYieldRate(result, workOrder),
            productionTime = calculateProductionTime(result),
            startTime = result.createDate?.format(DateTimeFormatter.ISO_TIME),
            endTime = result.updateDate?.format(DateTimeFormatter.ISO_TIME),
            worker = result.createUser,
            supervisor = getSupervisor(result),
            progressRate = result.progressRate,
            defectRate = result.defectRate,
            status = workOrder?.state,
            defectCause = result.defectCause,
            resultInfo = result.resultInfo,
            createDate = result.createDate?.format(DateTimeFormatter.ISO_DATE_TIME),
            updateDate = result.updateDate?.format(DateTimeFormatter.ISO_DATE_TIME),
            createUser = result.createUser,
            updateUser = result.updateUser
        )
    }

    /**
     * 기간별 생산실적 통계 조회
     */
    fun getProductionResultStatistics(fromDate: LocalDate, toDate: LocalDate): ProductionStatisticsDto {
        val currentUser = getCurrentUserPrincipal()
        val results = productionResultRepository.getProductionResultListByDateRange(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            fromDate = LocalDateTime.of(fromDate, LocalTime.MIN),
            toDate = LocalDateTime.of(toDate, LocalTime.MAX)
        )

        // 통계 계산 로직
        var totalPlanQty = 0.0
        var totalGoodQty = 0.0
        var totalDefectQty = 0.0
        val productionByDate = mutableMapOf<String, ProductionDailyStat>()
        val productionByProduct = mutableMapOf<String, ProductionProductStat>()

        // 각 생산실적에 대한 작업지시 정보 맵 미리 구축
        val workOrderMap = results.mapNotNull { it.workOrderId }
            .distinct()
            .mapNotNull { workOrderId ->
                workOrderRepository.findByWorkOrderId(workOrderId)?.let { workOrder ->
                    workOrderId to workOrder
                }
            }.toMap()

        results.forEach { result ->
            val workOrder = result.workOrderId?.let { workOrderMap[it] }

            // 기본 데이터 누적
            val planQty = workOrder?.orderQty ?: 0.0
            val goodQty = result.goodQty ?: 0.0
            val defectQty = result.defectQty ?: 0.0

            totalPlanQty += planQty
            totalGoodQty += goodQty
            totalDefectQty += defectQty

            // 일자별 통계
            val dateKey = result.createDate?.toLocalDate()?.format(DateTimeFormatter.ISO_DATE) ?: "Unknown"
            val dailyStat = productionByDate.getOrDefault(dateKey, ProductionDailyStat(dateKey, 0.0, 0.0, 0.0))
            dailyStat.planQty += planQty
            dailyStat.goodQty += goodQty
            dailyStat.defectQty += defectQty
            productionByDate[dateKey] = dailyStat

            // 제품별 통계
            val productId = workOrder?.productId ?: "Unknown"
            val productName = getProductName(productId)

            val productStat = productionByProduct.getOrDefault(productId,
                ProductionProductStat(productId, productName ?: "Unknown", 0.0, 0.0, 0.0))
            productStat.planQty += planQty
            productStat.goodQty += goodQty
            productStat.defectQty += defectQty
            productionByProduct[productId] = productStat
        }

        // 달성률 및 불량률 계산
        val achievementRate = if (totalPlanQty > 0) (totalGoodQty / totalPlanQty) * 100 else 0.0
        val defectRate = if ((totalGoodQty + totalDefectQty) > 0) (totalDefectQty / (totalGoodQty + totalDefectQty)) * 100 else 0.0

        return ProductionStatisticsDto(
            fromDate = fromDate.format(DateTimeFormatter.ISO_DATE),
            toDate = toDate.format(DateTimeFormatter.ISO_DATE),
            totalPlanQty = totalPlanQty,
            totalGoodQty = totalGoodQty,
            totalDefectQty = totalDefectQty,
            achievementRate = String.format("%.1f", achievementRate),
            defectRate = String.format("%.1f", defectRate),
            dailyStats = productionByDate.values.toList().sortedBy { it.date },
            productStats = productionByProduct.values.toList().sortedBy { it.productId }
        )
    }

    /**
     * 설비별 생산실적 통계 조회
     */
    fun getProductionResultByEquipment(fromDate: LocalDate, toDate: LocalDate): List<ProductionEquipmentStat> {
        val currentUser = getCurrentUserPrincipal()
        val results = productionResultRepository.getProductionResultByEquipment(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            fromDate = LocalDateTime.of(fromDate, LocalTime.MIN),
            toDate = LocalDateTime.of(toDate, LocalTime.MAX)
        )

        // 설비별 통계 계산
        val equipmentStats = mutableMapOf<String, ProductionEquipmentStat>()

        results.forEach { result ->
            val equipmentId = result.equipmentId ?: "Unknown"
            val equipmentName = getEquipmentName(equipmentId)

            val goodQty = result.goodQty ?: 0.0
            val defectQty = result.defectQty ?: 0.0

            val stat = equipmentStats.getOrDefault(equipmentId,
                ProductionEquipmentStat(equipmentId, equipmentName, 0.0, 0.0, 0.0, "0.0"))

            stat.goodQty += goodQty
            stat.defectQty += defectQty
            stat.totalQty = stat.goodQty + stat.defectQty

            if (stat.totalQty > 0) {
                stat.defectRate = String.format("%.1f", (stat.defectQty / stat.totalQty) * 100)
            }

            equipmentStats[equipmentId] = stat
        }

        return equipmentStats.values.toList().sortedByDescending { it.totalQty }
    }

    /**
     * 생산실적 저장 (생성/수정)
     * - 불량정보도 함께 저장할 수 있는 기능 추가
     */
    @Transactional
    fun saveProductionResult(
        createdRows: List<ProductionResultInput>? = null,
        updatedRows: List<ProductionResultUpdate>? = null,
        defectInfos: List<DefectInfoInput>? = null
    ): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()

            // 새로운 생산실적 저장
            createdRows?.forEach { input ->
                // 양품과 불량품 수량으로부터 진척률과 불량률 계산
                val goodQty = input.goodQty ?: 0.0
                val defectQty = input.defectQty ?: 0.0
                val totalQty = goodQty + defectQty

                // 작업지시 정보 조회하여 계획수량 대비 진척률 계산
                val workOrder = input.workOrderId?.let {
                    workOrderRepository.findBySiteAndCompCdAndWorkOrderId(
                        site = currentUser.getSite(),
                        compCd = currentUser.compCd,
                        workOrderId = it
                    )
                }

                val orderQty = workOrder?.orderQty ?: 0.0
                val progressRate = if (orderQty > 0) {
                    String.format("%.1f", (totalQty / orderQty) * 100.0)
                } else "0.0"

                val defectRate = if (totalQty > 0) {
                    String.format("%.1f", (defectQty / totalQty) * 100.0)
                } else "0.0"

                val newResult = ProductionResult().apply {
                    site = currentUser.getSite()
                    compCd = currentUser.compCd
                    prodResultId = "PR" + System.currentTimeMillis() // 임시 ID 생성 방식
                    workOrderId = input.workOrderId
                    this.goodQty = goodQty
                    this.defectQty = defectQty
                    this.progressRate = progressRate
                    this.defectRate = defectRate
                    equipmentId = input.equipmentId
                    resultInfo = input.resultInfo
                    defectCause = input.defectCause
                    flagActive = true // 항상 활성 상태로 생성
                    createCommonCol(currentUser)
                }

                val savedResult = productionResultRepository.save(newResult)

                // 불량정보가 있는 경우 함께 저장
                defectInfos?.filter { it.workOrderId == input.workOrderId }?.let { relatedDefectInfos ->
                    if (relatedDefectInfos.isNotEmpty()) {
                        defectInfoService.saveDefectInfoForProductionResult(
                            prodResultId = savedResult.prodResultId!!,
                            workOrderId = savedResult.workOrderId!!,
                            defectInputs = relatedDefectInfos
                        )
                    }
                }

                // 작업 상태 업데이트 (선택적)
                workOrder?.let {
                    if (it.state == "PLANNED" && totalQty > 0) {
                        it.state = "IN_PROGRESS"
                        it.updateCommonCol(currentUser)
                        workOrderRepository.save(it)
                    } else if (it.state == "IN_PROGRESS" && orderQty > 0 && totalQty >= orderQty) {
                        it.state = "COMPLETED"
                        it.updateCommonCol(currentUser)
                        workOrderRepository.save(it)
                    }
                }
            }

            // 기존 생산실적 업데이트
            updatedRows?.forEach { update ->
                try {
                    // 특정 조건에 맞는 생산실적을 직접 찾는 쿼리 사용
                    val existingResult = productionResultRepository.findBySiteAndCompCdAndProdResultId(
                        currentUser.getSite(),
                        currentUser.compCd,
                        update.prodResultId
                    )

                    existingResult?.let { result ->
                        // 업데이트된 양품과 불량품 수량을 가져옴
                        val goodQty = update.goodQty ?: result.goodQty ?: 0.0
                        val defectQty = update.defectQty ?: result.defectQty ?: 0.0
                        val totalQty = goodQty + defectQty

                        // 작업지시 정보 조회하여 계획수량 대비 진척률 계산
                        val workOrder = result.workOrderId?.let {
                            workOrderRepository.findBySiteAndCompCdAndWorkOrderId(
                                site = currentUser.getSite(),
                                compCd = currentUser.compCd,
                                workOrderId = it
                            )
                        }

                        val orderQty = workOrder?.orderQty ?: 0.0
                        val progressRate = if (orderQty > 0) {
                            String.format("%.1f", (totalQty / orderQty) * 100.0)
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
                            updateCommonCol(currentUser)
                        }

                        productionResultRepository.save(result)

                        // 작업 상태 업데이트 (선택적)
                        workOrder?.let { wo ->
                            if (wo.state == "PLANNED" && totalQty > 0) {
                                wo.state = "IN_PROGRESS"
                                wo.updateCommonCol(currentUser)
                                workOrderRepository.save(wo)
                            } else if (wo.state == "IN_PROGRESS" && orderQty > 0 && totalQty >= orderQty) {
                                wo.state = "COMPLETED"
                                wo.updateCommonCol(currentUser)
                                workOrderRepository.save(wo)
                            } else {

                            }
                        }
                    } ?: log.warn("업데이트할 생산실적을 찾을 수 없습니다: {}", update.prodResultId)
                } catch (e: Exception) {
                    log.error("생산실적 업데이트 중 오류 발생: {}", update.prodResultId, e)
                }
            }

            return true
        } catch (e: Exception) {
            log.error("생산실적 저장 중 오류 발생", e)
            return false
        }
    }

    /**
     * 생산실적을 소프트 삭제하는 메서드 (flagActive = false로 설정)
     * - 연관된 불량정보도 함께 비활성화 처리
     */
    @Transactional
    fun softDeleteProductionResult(prodResultId: String): Boolean {
        try {
            val currentUser = getCurrentUserPrincipal()

            // 특정 조건에 맞는 생산실적을 직접 찾는 쿼리 사용
            val existingResult = productionResultRepository.findBySiteAndCompCdAndProdResultId(
                currentUser.getSite(),
                currentUser.compCd,
                prodResultId
            )

            existingResult?.let {
                // flagActive를 false로 설정
                it.flagActive = false
                it.updateCommonCol(currentUser)
                productionResultRepository.save(it)

                // 관련 불량정보도 비활성화 처리 (선택적)
                try {
                    val defectInfos = defectInfoService.getDefectInfoByProdResultId(prodResultId)
                    defectInfos.forEach { defectInfo ->
                        defectInfoService.softDeleteDefectInfo(defectInfo.defectId!!)
                    }
                } catch (e: Exception) {
                    log.warn("관련 불량정보 삭제 중 오류 발생: {}", e.message)
                    // 불량정보 삭제 실패는 전체 프로세스를 실패로 간주하지 않음
                }

                return true
            }

            log.warn("삭제(비활성화)할 생산실적을 찾을 수 없습니다: {}", prodResultId)
            return false
        } catch (e: Exception) {
            log.error("생산실적 소프트 삭제 중 오류 발생", e)
            return false
        }
    }

    /**
     * 작업지시ID로 마지막 생산실적 조회
     * - 해당 작업지시의 가장 최근 생산실적을 조회
     */
    fun getLatestProductionResultByWorkOrder(workOrderId: String): ProductionResult? {
        val currentUser = getCurrentUserPrincipal()
        val results = productionResultRepository.getProductionResultsByWorkOrderId(
            site = currentUser.getSite(),
            compCd = currentUser.compCd,
            workOrderId = workOrderId
        )

        // 생성일 기준 내림차순 정렬된 결과에서 첫 번째 항목 반환
        return results.firstOrNull()
    }

    // 유틸리티 메소드들

    /**
     * 제품ID로 제품명 조회
     * 실제 구현에서는 ProductRepository를 통해 조회
     */
    private fun getProductName(productId: String): String? {
        // 실제 구현에서는 DB 조회
        return "제품 $productId"
    }

    private fun getEquipmentName(equipmentId: String): String {
        // 실제 구현에서는 설비 리포지토리에서 조회
        return "설비 $equipmentId"
    }

    private fun getFactoryId(equipmentId: String): String {
        // 실제 구현에서는 설비 정보에서 공장 ID 추출
        return equipmentId.split("-").firstOrNull() ?: "F001"
    }

    private fun getFactoryName(equipmentId: String): String {
        // 실제 구현에서는 공장 리포지토리에서 조회
        val factoryId = getFactoryId(equipmentId)
        return "공장 $factoryId"
    }

    private fun getLineId(equipmentId: String): String {
        // 실제 구현에서는 설비 정보에서 라인 ID 추출
        val parts = equipmentId.split("-")
        return if (parts.size > 1) parts[1] else "L001"
    }

    private fun getLineName(equipmentId: String): String {
        // 실제 구현에서는 라인 리포지토리에서 조회
        val lineId = getLineId(equipmentId)
        return "라인 $lineId"
    }

    private fun calculateInputAmount(result: ProductionResult): Double {
        // 실제 구현에서는 로직에 맞게 계산
        val goodQty = result.goodQty ?: 0.0
        val defectQty = result.defectQty ?: 0.0
        return goodQty + defectQty + (goodQty * 0.05) // 예: 로스율 5% 가정
    }

    private fun calculateOutputAmount(result: ProductionResult): Double {
        // 실제로는 산출량이 별도로 기록될 수 있음
        return result.goodQty ?: 0.0
    }

    private fun calculateYieldRate(result: ProductionResult, workOrder: WorkOrder?): String {
        val planQty = workOrder?.orderQty ?: 0.0
        val goodQty = result.goodQty ?: 0.0

        return if (planQty > 0) {
            String.format("%.1f", (goodQty / planQty) * 100)
        } else "0.0"
    }

    private fun calculateProductionTime(result: ProductionResult): String {
        val startTime = result.createDate
        val endTime = result.updateDate

        if (startTime != null && endTime != null) {
            val minutes = ChronoUnit.MINUTES.between(startTime, endTime)
            val hours = minutes / 60
            val mins = minutes % 60
            return String.format("%02d:%02d", hours, mins)
        }

        return "00:00"
    }

    private fun getSupervisor(result: ProductionResult): String {
        // 실제 구현에서는 관리자 정보 조회
        return result.updateUser ?: "Unknown"
    }
}