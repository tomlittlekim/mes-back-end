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
    var materialCategory: String? = null, // 제품 유형 필드 추가 - 검색용으로만 사용
    var shiftType: String? = null,
    var planStartDateFrom: LocalDate? = null,
    var planStartDateTo: LocalDate? = null,
    var planEndDateFrom: LocalDate? = null,  // 추가: 계획종료일 범위 시작
    var planEndDateTo: LocalDate? = null,    // 추가: 계획종료일 범위 끝
    var flagActive: Boolean? = null
)

// 조회 결과를 담을 DTO 클래스 정의
data class ProductionPlanDTO(
    val id: Int?,
    val site: String?,
    val compCd: String?,
    val prodPlanId: String?,
    val orderId: String?,
    val productId: String?,
    val shiftType: String?,
    val planQty: Double?,
    val planStartDate: LocalDateTime?,
    val planEndDate: LocalDateTime?,
    val createDate: LocalDateTime?,
    val createUser: String?,
    val updateDate: LocalDateTime?,
    val updateUser: String?,
    val flagActive: Boolean?,
    // 조인 대상 테이블 필드
    val productName: String?,
    val materialCategory: String?
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