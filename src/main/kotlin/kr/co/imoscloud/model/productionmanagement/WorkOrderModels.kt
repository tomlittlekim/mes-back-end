package kr.co.imoscloud.model.productionmanagement

// WorkOrder 관련 모델
data class WorkOrderFilter(
    var workOrderId: String? = null,
    var prodPlanId: String? = null,
    var productId: String? = null,
    var shiftType: String? = null,
    var state: List<String>? = null,
    var flagActive: Boolean? = null,
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

// 삭제 결과 모델 - ProductionPlan과 통일성을 맞춤
data class WorkOrderDeleteResult(
    val success: Boolean,
    val totalRequested: Int,
    val deletedCount: Int,
    val skippedCount: Int,
    val skippedWorkOrders: List<String>,
    val message: String
)

// 작업 시작/완료 결과 모델
data class WorkOrderOperationResult(
    val success: Boolean,
    val totalRequested: Int,
    val processedCount: Int,
    val skippedCount: Int,
    val skippedWorkOrders: List<String>,
    val message: String
)