package kr.co.imoscloud.model.productionmanagement

/**
 * 불량 정보 필터 - 기본 조회용
 */
data class DefectInfoFilter(
    var workOrderId: String? = null,
    var prodResultId: String? = null,
    var defectId: String? = null,
    var productId: String? = null,
    var productName: String? = null,
    var defectType: String? = null,
    var fromDate: String? = null,
    var toDate: String? = null,
    var state: String? = null,
    var flagActive: Boolean? = true
)

/**
 * 불량 정보 입력 - 신규 등록용
 */
data class DefectInfoInput(
    val workOrderId: String,
    val prodResultId: String,
    val productId: String? = null,
    val productName: String? = null,
    val defectQty: Double? = null,
    val defectType: String? = null,
    val defectReason: String? = null,
    val resultInfo: String? = null,
    val state: String? = null,
    val defectCause: String? = null,
    val flagActive: Boolean? = true
)

/**
 * 불량 정보 업데이트 - 수정용
 */
data class DefectInfoUpdate(
    val defectId: String,
    val workOrderId: String? = null,
    val prodResultId: String? = null,
    val productId: String? = null,
    val productName: String? = null,
    val defectQty: Double? = null,
    val defectType: String? = null,
    val defectReason: String? = null,
    val resultInfo: String? = null,
    val state: String? = null,
    val defectCause: String? = null,
    val flagActive: Boolean? = null
)

/**
 * 제품별 불량 통계
 */
data class DefectStatsByProductDto(
    val productId: String,
    val productName: String,
    val totalDefectQty: Double,
    val defectCount: Int,
    val defectTypes: List<DefectTypeCountDto>,
    val defectCauses: List<DefectCauseCountDto>
)

/**
 * 불량 유형별 통계
 */
data class DefectTypeCountDto(
    val defectType: String,
    val count: Int,
    val qty: Double,
    val percentage: Double
)

/**
 * 불량 원인별 통계
 */
data class DefectCauseCountDto(
    val cause: String,
    val count: Int,
    val qty: Double,
    val percentage: Double
)

/**
 * 원인별 불량 통계
 */
data class DefectStatsByCauseDto(
    val defectCause: String,
    val totalDefectQty: Double,
    val defectCount: Int,
    val products: List<ProductDefectCountDto>
)

/**
 * 제품별 불량 통계
 */
data class ProductDefectCountDto(
    val productId: String,
    val productName: String,
    val qty: Double,
    val count: Int,
    val percentage: Double
)