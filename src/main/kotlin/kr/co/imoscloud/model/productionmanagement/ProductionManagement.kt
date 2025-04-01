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
    var planStartDate: LocalDate? = null,
    var planEndDate: LocalDate? = null,
    var flagActive: Boolean? = null
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