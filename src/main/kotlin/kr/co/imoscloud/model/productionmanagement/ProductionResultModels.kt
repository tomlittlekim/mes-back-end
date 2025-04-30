package kr.co.imoscloud.model.productionmanagement

import java.time.LocalDateTime

/**
 * 생산실적 필터 클래스
 */
data class ProductionResultFilter(
    var workOrderId: String? = null,
    var prodResultId: String? = null,
    var productId: String? = null,
    var equipmentId: String? = null,
    var warehouseId: String? = null,
    var prodStartTimeFrom: LocalDateTime? = null,
    var prodStartTimeTo: LocalDateTime? = null,
    var prodEndTimeFrom: LocalDateTime? = null,
    var prodEndTimeTo: LocalDateTime? = null,
    var flagActive: Boolean? = null
)

/**
 * 생산실적 생성 입력 클래스
 */
data class ProductionResultInput(
    val workOrderId: String? = null,
    val productId: String? = null,
    val goodQty: Double? = null,
    val defectQty: Double? = null,
    val equipmentId: String? = null,
    val warehouseId: String? = null,
    val resultInfo: String? = null,
    val defectCause: String? = null,
    // 타입을 LocalDateTime에서 String으로 변경
    val prodStartTime: String? = null,
    val prodEndTime: String? = null,
    val flagActive: Boolean? = true
)