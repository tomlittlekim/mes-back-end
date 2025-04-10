package kr.co.imoscloud.model.productionmanagement

/**
 * 불량정보 필터 클래스
 */
data class DefectInfoFilter(
    val defectId: String? = null,
    val workOrderId: String? = null,
    val prodResultId: String? = null,
    val productId: String? = null,
    val productName: String? = null,
    val state: String? = null,
    val defectType: String? = null,
    val fromDate: String? = null,
    val toDate: String? = null,
    val flagActive: Boolean? = true
)

/**
 * 불량정보 DTO
 */
data class DefectInfoDto(
    val defectId: String? = null,
    val workOrderId: String? = null,
    val prodResultId: String? = null,
    val productId: String? = null,
    val defectName: String? = null,
    val defectQty: Double? = null,
    val defectCause: String? = null,
    val state: String? = null,
    val resultInfo: String? = null,
    val createDate: String? = null,
    val updateDate: String? = null,
    val createUser: String? = null,
    val updateUser: String? = null
)

/**
 * 불량정보 입력 클래스
 */
data class DefectInfoInput(
    val prodResultId: String? = null,
    val productId: String? = null,
    val productName: String? = null,
    val defectName: String? = null,
    val defectQty: Double = 0.0,
    val defectCause: String? = null,
    val state: String? = "NEW",
    val resultInfo: String? = null,
    val defectType: String? = null,
    val defectReason: String? = null,
    val flagActive: Boolean? = true
)

/**
 * 불량정보 수정 클래스
 */
data class DefectInfoUpdate(
    val defectId: String,
    val workOrderId: String? = null,
    val prodResultId: String? = null,
    val productId: String? = null,
    val productName: String? = null,
    val defectName: String? = null,
    val defectQty: Double? = null,
    val defectCause: String? = null,
    val state: String? = null,
    val resultInfo: String? = null,
    val defectType: String? = null,
    val defectReason: String? = null,
    val flagActive: Boolean? = null
)

/**
 * 제품별 불량 통계 DTO
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
 * 불량 유형별 통계 DTO
 */
data class DefectTypeCountDto(
    val defectType: String,
    val count: Int,
    val qty: Double,
    val percentage: Double
)

/**
 * 불량 원인별 통계 DTO
 */
data class DefectCauseCountDto(
    val cause: String,
    val count: Int,
    val qty: Double,
    val percentage: Double
)

/**
 * 원인별 불량 통계 DTO
 */
data class DefectStatsByCauseDto(
    val defectCause: String,
    val totalDefectQty: Double,
    val defectCount: Int,
    val products: List<ProductDefectCountDto>
)

/**
 * 제품별 불량 개수 DTO
 */
data class ProductDefectCountDto(
    val productId: String,
    val productName: String,
    val qty: Double,
    val count: Int,
    val percentage: Double
)