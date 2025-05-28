package kr.co.imoscloud.model.kpisetting

// 지점 및 회사 정보 응답 모델
data class BranchModel(
    val id: String,
    val name: String,
    val companies: List<KpiCompanyModel>
)

data class KpiCompanyModel(
    val id: String,
    val name: String
)

// KPI 지표 정보 응답 모델
data class KpiIndicatorModel(
    val kpiIndicatorCd: String,
    val kpiIndicatorNm: String,
    val description: String?,
    val categoryCd: String,
    val categoryNm: String?,
    val targetValue: Double?,
    val unit: String?,
    val chartType: String?
)

// KPI 지표와 카테고리 정보를 함께 포함하는 모델
data class KpiIndicatorWithCategoryModel(
    val kpiIndicatorCd: String,
    val kpiIndicatorNm: String?,
    val description: String?,
    val categoryCd: String,
    val categoryNm: String?,
    val targetValue: Double?,
    val unit: String?,
    val chartType: String?
)

// KPI 구독 정보 응답 모델
data class KpiSubscriptionModel(
    val site: String,
    val compCd: String,
    val kpiIndicatorCd: String,
    val categoryId: String,
    val description: String?,
    val sort: Int?,
    val flagActive: Boolean? = true
)

// KPI 설정 저장 입력 모델
data class KpiSettingInput(
    val site: String,
    val compCd: String,
    val kpiIndicatorCd: String,
    val categoryId: String,
    val description: String?,
    val sort: Int?,
    val flagActive: Boolean? = true
)

// KPI 설정 저장 결과 모델
data class KpiSettingResult(
    val success: Boolean,
    val message: String
) 