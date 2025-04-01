package kr.co.imoscloud.service.productionmanagement

import kr.co.imoscloud.entity.productionmanagement.ProductionResult
import kr.co.imoscloud.entity.productionmanagement.WorkOrder
import kr.co.imoscloud.model.productionmanagement.*
import kr.co.imoscloud.repository.productionmanagement.ProductionResultInquiryRepository
import kr.co.imoscloud.repository.productionmanagement.WorkOrderRepository
import kr.co.imoscloud.util.SecurityUtils.getCurrentUserPrincipal
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 생산실적조회 서비스
 * - 생산실적 조회, 검색, 통계 기능을 제공
 */
@Service
class ProductionResultInquiryService(
    val productionResultInquiryRepository: ProductionResultInquiryRepository,
    val workOrderRepository: WorkOrderRepository
) {
    /**
     * 기본 생산실적 조회
     * - 기존 getProductionResults와 유사하나 DTO 변환 로직 추가
     */
    fun getProductionResultList(filter: ProductionResultInquiryFilter): List<ProductionResultSummaryDto> {
        val currentUser = getCurrentUserPrincipal()

        val results = productionResultInquiryRepository.getProductionResultList(
            site = currentUser.getSite(),
            compCd = currentUser.getCompCd(),
            workOrderId = filter.workOrderId,
            prodResultId = filter.prodResultId,
            productId = filter.productId,
            equipmentId = filter.equipmentId,
            fromDate = filter.fromDate?.let {
                LocalDateTime.of(it, LocalTime.MIN)
            },
            toDate = filter.toDate?.let {
                LocalDateTime.of(it, LocalTime.MAX)
            },
            status = filter.status,
            flagActive = filter.flagActive ?: true
        )

        // 생산실적 결과를 요약 DTO로 변환
        return results.map { result ->
            val workOrder = result.workOrderId?.let {
                workOrderRepository.findByWorkOrderId(it)
            }

            // 제품명 가져오기 (별도 로직으로 구현 필요)
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
     * - ID로 단일 생산실적의 상세 정보 조회
     */
    fun getProductionResultDetail(prodResultId: String): ProductionResultInquiryDto? {
        val currentUser = getCurrentUserPrincipal()
        val result = productionResultInquiryRepository.findDetailByProdResultId(
            site = currentUser.getSite(),
            compCd = currentUser.getCompCd(),
            prodResultId = prodResultId
        ) ?: return null

        val workOrder = result.workOrderId?.let {
            workOrderRepository.findByWorkOrderId(it)
        }

        val productionDate = result.createDate?.toLocalDate() ?: LocalDate.now()

        // 제품명 조회
        val productName = workOrder?.productId?.let { getProductName(it) }

        return ProductionResultInquiryDto(
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
        val results = productionResultInquiryRepository.getProductionResultListByDateRange(
            site = currentUser.getSite(),
            compCd = currentUser.getCompCd(),
            fromDate = LocalDateTime.of(fromDate, LocalTime.MIN),
            toDate = LocalDateTime.of(toDate, LocalTime.MAX)
        )

        // 통계 계산 로직
        var totalPlanQty = 0.0
        var totalGoodQty = 0.0
        var totalDefectQty = 0.0
        val productionByDate = mutableMapOf<String, ProductionDailyStat>()
        val productionByProduct = mutableMapOf<String, ProductionProductStat>()

        results.forEach { result ->
            val workOrder = result.workOrderId?.let {
                workOrderRepository.findByWorkOrderId(it)
            }

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
            dailyStats = productionByDate.values.toList(),
            productStats = productionByProduct.values.toList()
        )
    }

    /**
     * 설비별 생산실적 통계 조회
     */
    fun getProductionResultByEquipment(fromDate: LocalDate, toDate: LocalDate): List<ProductionEquipmentStat> {
        val currentUser = getCurrentUserPrincipal()
        val results = productionResultInquiryRepository.getProductionResultListByDateRange(
            site = currentUser.getSite(),
            compCd = currentUser.getCompCd(),
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

        return equipmentStats.values.toList()
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
        return equipmentId.split("-").firstOrNull() ?: ""
    }

    private fun getFactoryName(equipmentId: String): String {
        // 실제 구현에서는 공장 리포지토리에서 조회
        val factoryId = getFactoryId(equipmentId)
        return "공장 $factoryId"
    }

    private fun getLineId(equipmentId: String): String {
        // 실제 구현에서는 설비 정보에서 라인 ID 추출
        val parts = equipmentId.split("-")
        return if (parts.size > 1) parts[1] else ""
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