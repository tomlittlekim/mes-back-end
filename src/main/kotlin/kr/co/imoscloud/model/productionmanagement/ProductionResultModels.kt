package kr.co.imoscloud.model.productionmanagement

import java.time.LocalDate

/**
 * 생산실적 필터 클래스
 */
data class ProductionResultFilter(
    var workOrderId: String? = null,
    var prodResultId: String? = null,
    var equipmentId: String? = null,
    var planStartDateFrom: LocalDate? = null, // 시작일
    var planStartDateTo: LocalDate? = null,   // 종료일
    var flagActive: Boolean? = null
)

/**
 * 생산실적 조회용 필터 클래스
 */
data class ProductionResultInquiryFilter(
    val prodResultId: String? = null,
    val workOrderId: String? = null,
    val productId: String? = null,
    val equipmentId: String? = null,
    val fromDate: String? = null,
    val toDate: String? = null,
    val status: String? = null,
    val flagActive: Boolean? = true
)

/**
 * 생산실적 생성 입력 클래스
 */
data class ProductionResultInput(
    val workOrderId: String? = null,
    val goodQty: Double? = null,
    val defectQty: Double? = null,
    val equipmentId: String? = null,
    val resultInfo: String? = null,
    val defectCause: String? = null,
    val flagActive: Boolean? = true
)

/**
 * 생산실적 수정 입력 클래스
 */
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
 * 생산실적 요약 DTO 클래스
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
 * 생산실적 상세 DTO 클래스
 */
data class ProductionResultDetailDto(
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
 * 생산실적 기본 DTO
 */
data class ProductionResultDto(
    val id: Int? = null,
    val workOrderId: String? = null,
    val prodResultId: String? = null,
    val goodQty: Double? = null,
    val defectQty: Double? = null,
    val progressRate: String? = null,
    val defectRate: String? = null,
    val equipmentId: String? = null,
    val resultInfo: String? = null,
    val defectCause: String? = null,
    val createUser: String? = null,
    val createDate: String? = null,
    val updateUser: String? = null,
    val updateDate: String? = null,
    val flagActive: Boolean? = true
)