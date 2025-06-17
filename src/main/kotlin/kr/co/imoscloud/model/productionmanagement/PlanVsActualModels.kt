package kr.co.imoscloud.model.productionmanagement

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// 인터페이스 기반 프로젝션으로 변경
interface PlanVsActualResponseDto {
    val prodPlanId: String
    val planQty: Number
    val totalOrderQty: Number
    val completedOrderQty: Number
    val achievementRate: Number
    val materialName: String
    val systemMaterialId: String
}

interface PeriodicProductionResponseDto {
    val materialName: String
    val totalGoodQty: String
    val totalDefectQty: Number
    val totalDefectRate: String
    val unit: String
    val productId: String
}

// GraphQL에서 사용할 구현체 클래스 (필요시 사용)
data class PlanVsActualResponseDtoImpl(
    override val prodPlanId: String,
    override val planQty: Number,
    override val totalOrderQty: Number,
    override val completedOrderQty: Number,
    override val achievementRate: Number,
    override val materialName: String,
    override val systemMaterialId: String
) : PlanVsActualResponseDto

// 숫자 타입 확장 함수 (DTO -> GraphQL 응답 변환용)
fun PlanVsActualResponseDto.toGraphQLResponse(): PlanVsActualGraphQLDto {
    return PlanVsActualGraphQLDto(
        prodPlanId = this.prodPlanId,
        planQty = this.planQty.toInt(),
        totalOrderQty = this.totalOrderQty.toInt(),
        completedOrderQty = this.completedOrderQty.toInt(),
        achievementRate = this.achievementRate.toDouble(),
        materialName = this.materialName,
        systemMaterialId = this.systemMaterialId
    )
}

// GraphQL 응답용 DTO
data class PlanVsActualGraphQLDto(
    val prodPlanId: String,
    val planQty: Int,
    val totalOrderQty: Int,
    val completedOrderQty: Int,
    val achievementRate: Double,
    val materialName: String,
    val systemMaterialId: String
)

data class PlanVsActualFilter(
    val systemMaterialIds: List<String>? = null,
    val startDate: String? = null,
    val endDate: String? = null,
)

data class defectInfoResponse(
    val defectQty: String? = null,
    val createDate: String? = null,
    val codeName: String? = null,
    val codeDesc: String? = null,
) {
    companion object {
        // Projection용 from 메서드만 유지
        fun from(projection: DefectInfoProjection): defectInfoResponse {
            return defectInfoResponse(
                defectQty = projection.getDefectQty(),
                createDate = projection.getCreateDate()?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                codeName = projection.getCodeName(),
                codeDesc = projection.getCodeDesc()
            )
        }
    }
}


interface DefectInfoProjection {
    fun getDefectQty(): String?
    fun getCreateDate(): LocalDateTime?
    fun getCodeName(): String?
    fun getCodeDesc(): String?
}

//data class PeriodicProductionFilter(
//    val systemMaterialIds: List<String>? = null,
//    val startDate: String? = null,
//    val endDate: String? = null,
//)