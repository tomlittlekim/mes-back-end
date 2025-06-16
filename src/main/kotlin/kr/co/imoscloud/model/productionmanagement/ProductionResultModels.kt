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
    // 생산시작일시는 필수, 생산종료일시는 비필수 (모바일에서 생산시작 시에는 null 가능)
    val prodStartTime: String,
    val prodEndTime: String? = null,
    val flagActive: Boolean? = true,
    // 해당 생산실적에 속하는 불량정보 목록 (개선사항)
    val defectInfos: List<DefectInfoInput>? = null
)