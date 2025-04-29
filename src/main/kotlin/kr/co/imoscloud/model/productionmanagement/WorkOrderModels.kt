package kr.co.imoscloud.model.productionmanagement

// WorkOrder 관련 모델
data class WorkOrderFilter(
    var workOrderId: String? = null,
    var prodPlanId: String? = null,
    var productId: String? = null,
    var shiftType: String? = null,
    var state: List<String>? = null,
    var flagActive: Boolean? = null,
    var planStartDateFrom: String? = null,
    var planStartDateTo: String? = null,
    var planEndDateFrom: String? = null,
    var planEndDateTo: String? = null,
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