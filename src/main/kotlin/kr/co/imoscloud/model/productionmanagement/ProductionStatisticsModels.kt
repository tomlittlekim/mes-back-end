package kr.co.imoscloud.model.productionmanagement

/**
 * 생산통계 DTO
 */
data class ProductionStatisticsDto(
    val fromDate: String,
    val toDate: String,
    val totalPlanQty: Double = 0.0,
    val totalGoodQty: Double = 0.0,
    val totalDefectQty: Double = 0.0,
    val achievementRate: String = "0.0",
    val defectRate: String = "0.0",
    val dailyStats: List<ProductionDailyStat> = emptyList(),
    val productStats: List<ProductionProductStat> = emptyList()
)

/**
 * 일별 생산통계
 */
data class ProductionDailyStat(
    val date: String,
    var planQty: Double = 0.0,
    var goodQty: Double = 0.0,
    var defectQty: Double = 0.0
)

/**
 * 제품별 생산통계
 */
data class ProductionProductStat(
    val productId: String,
    val productName: String,
    var planQty: Double = 0.0,
    var goodQty: Double = 0.0,
    var defectQty: Double = 0.0
)

/**
 * 설비별 생산통계
 */
data class ProductionEquipmentStat(
    val equipmentId: String,
    val equipmentName: String? = null,
    var goodQty: Double = 0.0,
    var defectQty: Double = 0.0,
    var totalQty: Double = 0.0,
    var defectRate: String = "0.0"
)