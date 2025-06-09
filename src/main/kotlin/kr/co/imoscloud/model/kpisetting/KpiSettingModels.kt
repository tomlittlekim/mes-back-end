package kr.co.imoscloud.model.kpisetting

/**
 * 지점 정보 응답 모델
 * 지점 ID, 이름 및 연관된 회사 목록을 포함
 */
data class BranchModel(
    val id: String,      // 지점 ID
    val name: String,    // 지점 이름
    val companies: List<KpiCompanyModel>  // 해당 지점의 회사 목록
)

/**
 * 회사 정보 모델
 * KPI 설정에서 사용
 */
data class KpiCompanyModel(
    val id: String,     // 회사 코드
    val name: String    // 회사 이름
)

/**
 * KPI 지표 정보 응답 모델
 * 기본 지표 정보 포함
 */
data class KpiIndicatorModel(
    val kpiIndicatorCd: String,   // 지표 코드
    val kpiIndicatorNm: String,   // 지표 이름
    val description: String?,     // 지표 설명
    val categoryCd: String,       // 카테고리 코드
    val categoryNm: String?,      // 카테고리 이름
    val unit: String?,            // 단위
    val chartType: String?        // 차트 타입
)

/**
 * KPI 지표와 카테고리 정보를 함께 포함하는 모델
 * 카테고리 정보와 조인된 결과 표현
 */
data class KpiIndicatorWithCategoryModel(
    val kpiIndicatorCd: String,   // 지표 코드
    val kpiIndicatorNm: String?,  // 지표 이름
    val description: String?,     // 지표 설명
    val categoryCd: String,       // 카테고리 코드
    val categoryNm: String?,      // 카테고리 이름
    val unit: String?,            // 단위
    val chartType: String?        // 차트 타입
)

/**
 * KPI 지표와 카테고리, 구독 정보를 함께 포함하는 모델
 * 카테고리 정보와 회사별 구독 정보(목표값, 활성화 여부 등)를 조인한 결과 표현
 */
data class KpiIndicatorWithCategoryAndSubscriptionModel(
    val kpiIndicatorCd: String,   // 지표 코드
    val kpiIndicatorNm: String?,  // 지표 이름
    val description: String?,     // 지표 설명
    val categoryCd: String,       // 카테고리 코드
    val categoryNm: String?,      // 카테고리 이름
    val unit: String?,            // 단위
    val chartType: String?,       // 차트 타입
    val targetValue: Double?,     // 회사별 목표값
    val flagActive: Boolean?      // 활성화 여부
)

/**
 * KPI 구독 정보 응답 모델
 * 회사별 KPI 구독 정보
 */
data class KpiSubscriptionModel(
    val site: String,             // 사이트 구분값
    val compCd: String,           // 회사 코드
    val kpiIndicatorCd: String,   // 지표 코드
    val categoryId: String,       // 카테고리 ID
    val targetValue: Double? = null, //목표값
    val description: String?,     // 커스텀 설명
    val sort: Int?,               // 정렬 순서
    val flagActive: Boolean? = true  // 활성화 여부
)

/**
 * KPI 설정 저장 입력 모델
 * 구독 정보 생성/수정 시 사용
 */
data class KpiSettingInput(
    val site: String,             // 사이트 구분값
    val compCd: String,           // 회사 코드
    val kpiIndicatorCd: String,   // 지표 코드
    val categoryId: String,       // 카테고리 ID
    val targetValue: Double? = null,     // 목표값
    val description: String?,     // 커스텀 설명
    val sort: Int?,               // 정렬 순서
    val flagActive: Boolean? = true  // 활성화 여부
)

/**
 * KPI 설정 저장 결과 모델
 */
data class KpiSettingResult(
    val success: Boolean,  // 성공 여부
    val message: String    // 결과 메시지
) 