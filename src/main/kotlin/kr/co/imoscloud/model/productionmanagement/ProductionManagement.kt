// ProductionModels.kt
package kr.co.imoscloud.model.productionmanagement

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ProductionPlan 관련 모델
data class ProductionPlanFilter(
    var prodPlanId: String? = null,
    var orderId: String? = null,
    var productId: String? = null,
    var planStartDateFrom: LocalDate? = null,  // 변경됨
    var planStartDateTo: LocalDate? = null,    // 변경됨
    var flagActive: Boolean? = null,
)

data class ProductionPlanInput(
    val orderId: String? = null,
    val productId: String? = null,
    val planQty: Double? = null,
    val planStartDate: String? = null,
    val planEndDate: String? = null,
    val flagActive: Boolean? = true
) {
    fun toLocalDateTimes(): Pair<LocalDateTime?, LocalDateTime?> {
        val startDate = planStartDate?.let {
            val date = LocalDate.parse(it)
            LocalDateTime.of(date, LocalTime.MIN)
        }

        val endDate = planEndDate?.let {
            val date = LocalDate.parse(it)
            LocalDateTime.of(date, LocalTime.MAX)
        }

        return Pair(startDate, endDate)
    }
}

data class ProductionPlanUpdate(
    val prodPlanId: String,
    val orderId: String? = null,
    val productId: String? = null,
    val planQty: Double? = null,
    val planStartDate: String? = null,
    val planEndDate: String? = null,
    val flagActive: Boolean? = null
) {
    fun toLocalDateTimes(): Pair<LocalDateTime?, LocalDateTime?> {
        val startDate = planStartDate?.let {
            val date = LocalDate.parse(it)
            LocalDateTime.of(date, LocalTime.MIN)
        }

        val endDate = planEndDate?.let {
            val date = LocalDate.parse(it)
            LocalDateTime.of(date, LocalTime.MAX)
        }

        return Pair(startDate, endDate)
    }
}

// WorkOrder 관련 모델
data class WorkOrderFilter(
    var workOrderId: String? = null,
    var prodPlanId: String? = null,
    var productId: String? = null,
    var shiftType: String? = null,
    var state: String? = null,
    var flagActive: Boolean? = null
)

data class WorkOrderInput(
    val prodPlanId: String? = null,
    val productId: String? = null,
    val orderQty: Double? = null,
    val shiftType: String? = null,
    val state: String? = null,
    val flagActive: Boolean? = true
)

data class WorkOrderUpdate(
    val workOrderId: String,
    val prodPlanId: String? = null,
    val productId: String? = null,
    val orderQty: Double? = null,
    val shiftType: String? = null,
    val state: String? = null,
    val flagActive: Boolean? = null
)

// ProductionResult 관련 모델
data class ProductionResultFilter(
    var workOrderId: String? = null,
    var prodResultId: String? = null,
    var equipmentId: String? = null,
    var flagActive: Boolean? = null
)

data class ProductionResultInput(
    val workOrderId: String? = null,
    val goodQty: Double? = null,
    val defectQty: Double? = null,
    val equipmentId: String? = null,
    val resultInfo: String? = null,
    val defectCause: String? = null,
    val flagActive: Boolean? = true
)

data class ProductionResultUpdate(
    val prodResultId: String,
    val workOrderId: String? = null,
    val goodQty: Double? = null,
    val defectQty: Double? = null,
    val equipmentId: String? = null,
    val resultInfo: String? = null,
    val defectCause: String? = null,
    val flagActive: Boolean? = null
)

/**
 * 생산실적 조회 필터
 */
data class ProductionResultInquiryFilter(
    val workOrderId: String? = null,
    val prodResultId: String? = null,
    val productId: String? = null,
    val equipmentId: String? = null,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val status: String? = null,
    val flagActive: Boolean? = true
)

/**
 * 생산실적 요약 DTO
 */
data class ProductionResultSummaryDto(
    val id: Int? = null,
    val prodResultId: String? = null,
    val workOrderId: String? = null,
    val productId: String? = null,
    val productName: String? = null,
    val equipmentId: String? = null,
    val equipmentName: String? = null,
    val productionDate: String? = null,
    val planQuantity: Double? = null,
    val actualQuantity: Double? = null,
    val defectQuantity: Double? = null,
    val progressRate: String? = null,
    val defectRate: String? = null,
    val worker: String? = null,
    val status: String? = null,
    val createDate: String? = null,
    val updateDate: String? = null
)

/**
 * 생산실적 상세 DTO
 */
data class ProductionResultInquiryDto(
    val id: Int? = null,
    val prodResultId: String? = null,
    val workOrderId: String? = null,
    val productId: String? = null,
    val productName: String? = null,
    val factoryId: String? = null,
    val factoryName: String? = null,
    val lineId: String? = null,
    val lineName: String? = null,
    val equipmentId: String? = null,
    val equipmentName: String? = null,
    val productionDate: String? = null,
    val planQuantity: Double? = null,
    val goodQuantity: Double? = null,
    val defectQuantity: Double? = null,
    val inputAmount: Double? = null,
    val outputAmount: Double? = null,
    val yieldRate: String? = null,
    val productionTime: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val worker: String? = null,
    val supervisor: String? = null,
    val progressRate: String? = null,
    val defectRate: String? = null,
    val status: String? = null,
    val defectCause: String? = null,
    val resultInfo: String? = null,
    val createDate: String? = null,
    val updateDate: String? = null,
    val createUser: String? = null,
    val updateUser: String? = null
)

/**
 * 통계 데이터 클래스들
 */
data class ProductionStatisticsDto(
    val fromDate: String,
    val toDate: String,
    val totalPlanQty: Double,
    val totalGoodQty: Double,
    val totalDefectQty: Double,
    val achievementRate: String,
    val defectRate: String,
    val dailyStats: List<ProductionDailyStat>,
    val productStats: List<ProductionProductStat>
)

data class ProductionDailyStat(
    val date: String,
    var planQty: Double,
    var goodQty: Double,
    var defectQty: Double
)

data class ProductionProductStat(
    val productId: String,
    val productName: String,
    var planQty: Double,
    var goodQty: Double,
    var defectQty: Double
)

data class ProductionEquipmentStat(
    val equipmentId: String,
    val equipmentName: String,
    var goodQty: Double,
    var defectQty: Double,
    var totalQty: Double,
    var defectRate: String
)