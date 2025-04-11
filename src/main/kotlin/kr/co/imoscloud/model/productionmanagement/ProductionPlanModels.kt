package kr.co.imoscloud.model.productionmanagement

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ProductionPlan 관련 모델
data class ProductionPlanFilter(
    var prodPlanId: String? = null,
    var orderId: String? = null,
    var productId: String? = null,
    var productName: String? = null, // 제품명 필드 추가
    var shiftType: String? = null,
    var planStartDateFrom: LocalDate? = null,
    var planStartDateTo: LocalDate? = null,
    var flagActive: Boolean? = null
)

data class ProductionPlanInput(
    val orderId: String? = null,
    val productId: String? = null,
    val productName: String? = null, // 제품명 필드 추가
    val shiftType: String? = null,
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
    val productName: String? = null, // 제품명 필드 추가
    val shiftType: String? = null,
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