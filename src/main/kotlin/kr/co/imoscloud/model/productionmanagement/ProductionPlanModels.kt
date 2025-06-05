package kr.co.imoscloud.model.productionmanagement

import com.fasterxml.jackson.annotation.JsonIgnore
import kr.co.imoscloud.util.DateUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// GraphQL Input 타입 (다른 productionmanagement 클래스들과 일관성 유지)
data class ProductionPlanFilter(
    val prodPlanId: String? = null,
    val orderId: String? = null,
    val orderDetailId: String? = null,
    val productId: String? = null,
    val productName: String? = null,
    val materialCategory: String? = null,
    val shiftType: String? = null,
    val planStartDateFrom: String? = null,
    val planStartDateTo: String? = null,
    val planEndDateFrom: String? = null,
    val planEndDateTo: String? = null,
    val flagActive: Boolean? = null
)

// 조회 결과를 담을 DTO 클래스 정의
data class ProductionPlanDTO(
    val id: Int?,
    val site: String?,
    val compCd: String?,
    val prodPlanId: String?,
    val orderId: String?,
    val orderDetailId: String?,
    val productId: String?,
    val shiftType: String?,
    val planQty: Double?,
    @JsonIgnore
    val planStartDateTime: LocalDateTime?,
    @JsonIgnore
    val planEndDateTime: LocalDateTime?,
    val createDate: LocalDateTime?,
    val createUser: String?,
    val updateDate: LocalDateTime?,
    val updateUser: String?,
    val flagActive: Boolean?,
    // 조인 대상 테이블 필드
    val productName: String?,
    val materialCategory: String?
) {
    // 계획시작일시 - 시간 정보(시, 분) 포함 형식으로 반환
    val planStartDate: String?
        get() = DateUtils.formatLocalDateTimeShort(planStartDateTime)
    
    // 계획종료일시 - 시간 정보(시, 분) 포함 형식으로 반환
    val planEndDate: String?
        get() = DateUtils.formatLocalDateTimeShort(planEndDateTime)
}

data class ProductionPlanInput(
    val orderId: String? = null,
    val orderDetailId: String? = null,
    val productId: String? = null,
    val productName: String? = null, // 제품명 필드 추가
    val shiftType: String? = null,
    val planQty: Double? = null,
    val planStartDate: String? = null,
    val planEndDate: String? = null,
    val flagActive: Boolean? = true
) {
    fun toLocalDateTimes(): Pair<LocalDateTime?, LocalDateTime?> {
        val startDate = planStartDate?.let { parseDateTimeString(it) }
        val endDate = planEndDate?.let { parseDateTimeString(it) }
        return Pair(startDate, endDate)
    }

    private fun parseDateTimeString(dateTimeStr: String): LocalDateTime? {
        return try {
            // 먼저 날짜와 시간이 모두 포함된 형태인지 확인 (예: "2024-01-01T14:30" 또는 "2024-01-01 14:30")
            when {
                dateTimeStr.contains("T") -> {
                    // ISO 형태 (2024-01-01T14:30)
                    if (dateTimeStr.length == 16) {
                        // 초 없이 분까지만 (2024-01-01T14:30)
                        LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                    } else {
                        // 기본 ISO 파싱 시도
                        LocalDateTime.parse(dateTimeStr)
                    }
                }
                dateTimeStr.contains(" ") -> {
                    // 공백으로 구분된 형태 (2024-01-01 14:30)
                    LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                }
                else -> {
                    // 날짜만 있는 경우 (2024-01-01) - 00:00:00으로 설정
                    val date = LocalDate.parse(dateTimeStr)
                    LocalDateTime.of(date, LocalTime.MIN)
                }
            }
        } catch (e: Exception) {
            // 파싱 실패 시 null 반환
            null
        }
    }
}

data class ProductionPlanUpdate(
    val prodPlanId: String,
    val orderId: String? = null,
    val orderDetailId: String? = null,
    val productId: String? = null,
    val productName: String? = null, // 제품명 필드 추가
    val shiftType: String? = null,
    val planQty: Double? = null,
    val planStartDate: String? = null,
    val planEndDate: String? = null,
    val flagActive: Boolean? = null
) {
    fun toLocalDateTimes(): Pair<LocalDateTime?, LocalDateTime?> {
        val startDate = planStartDate?.let { parseDateTimeString(it) }
        val endDate = planEndDate?.let { parseDateTimeString(it) }
        return Pair(startDate, endDate)
    }

    private fun parseDateTimeString(dateTimeStr: String): LocalDateTime? {
        return try {
            // 먼저 날짜와 시간이 모두 포함된 형태인지 확인 (예: "2024-01-01T14:30" 또는 "2024-01-01 14:30")
            when {
                dateTimeStr.contains("T") -> {
                    // ISO 형태 (2024-01-01T14:30)
                    if (dateTimeStr.length == 16) {
                        // 초 없이 분까지만 (2024-01-01T14:30)
                        LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                    } else {
                        // 기본 ISO 파싱 시도
                        LocalDateTime.parse(dateTimeStr)
                    }
                }
                dateTimeStr.contains(" ") -> {
                    // 공백으로 구분된 형태 (2024-01-01 14:30)
                    LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                }
                else -> {
                    // 날짜만 있는 경우 (2024-01-01) - 00:00:00으로 설정
                    val date = LocalDate.parse(dateTimeStr)
                    LocalDateTime.of(date, LocalTime.MIN)
                }
            }
        } catch (e: Exception) {
            // 파싱 실패 시 null 반환
            null
        }
    }
}

// 생산계획 삭제 결과를 담는 데이터 클래스
data class ProductionPlanDeleteResult(
    val success: Boolean,
    val totalRequested: Int,
    val deletedCount: Int,
    val skippedCount: Int,
    val skippedPlans: List<String>,
    val message: String
)