package kr.co.imoscloud.model.kpi

/**
 * =========== KPI 차트 데이터 관련 모델 ===========
 */

/**
 * KPI 지표별 개별 필터 입력 모델
 * kpiIndicatorCd가 null이면 모든 구독 지표에 대해 조회
 */
data class KpiSubscriptionFilter(
    val kpiIndicatorCd: String? = null,  // KPI 지표 코드 (선택적)
    val date: String,                    // 기준 날짜
    val range: String                    // 조회 범위 (day, week, month)
)

/**
 * KPI 차트 데이터 응답 모델
 */
data class KpiChartData(
    val kpiIndicatorCd: String,     // 지표 코드
    val kpiTitle: String,           // 지표 제목
    val categoryCd: String,         // 카테고리 코드
    val categoryNm: String? = null, // 카테고리 이름
    val chartType: String,          // 차트 타입 (line, bar 등)
    val unit: String? = null,       // 단위
    val targetValue: Double? = null, // 목표값
    val chartData: List<Map<String, Any>> // 차트 데이터
)

/**
 * =========== 기타 유틸리티 모델 ===========
 */

/**
 * 파라미터 모델 (데이터 조회 시 사용)
 */
data class KpiParams(
    val daysRange: Long,    // 조회 일수 범위
    val groupKey: String,   // 그룹화 키
    val substrStart: Int,   // 문자열 자르기 시작 위치
    val substrLength: Int   // 문자열 자르기 길이
) 