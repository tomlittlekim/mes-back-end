package kr.co.imoscloud.exception

enum class ErrorCode(
    val status: Int,
    val code: String,
    val message: String
) {

    //유저 관련 정보
    COMPANY_NOT_FOUND(403, "A001", "회사명을 찾을 수 없습니다."),

    //기기 관련 정보
    DEVICE_NOT_FOUND(403,"E001","기기를 찾을 수 없습니다."),

    //업체 관련 정보
    NOT_EXIST_VENDOR_ID(400,"V001", "거래처ID가 존재하지 않습니다."),
    NOT_EXIST_VENDOR_NAME(400,"V002","거래처명이 존재하지 않습니다."),
    NOT_EXIST_BUSINESS_REG_NO(400,"V003","사업자 번호가 존재하지 않습니다."),

    //인증 관련 정보
    USER_NOT_FOUND(401, "AUTH001", "사용자 정보를 찾을 수 없습니다. 로그인이 필요합니다."),

    //생산계획 관련 정보
    PRODUCTION_PLAN_NOT_FOUND(404, "PP001", "생산계획을 찾을 수 없습니다."),
    PRODUCTION_PLAN_SAVE_FAILED(500, "PP002", "생산계획 저장 중 오류가 발생했습니다."),

    //작업지시 관련 정보
    WORK_ORDER_NOT_FOUND(404, "WO001", "작업지시를 찾을 수 없습니다."),
    WORK_ORDER_SAVE_FAILED(500, "WO002", "작업지시 저장 중 오류가 발생했습니다."),

    //불량정보 관련 정보
    DEFECT_INFO_NOT_FOUND(404, "DF001", "불량정보를 찾을 수 없습니다."),
    DEFECT_INFO_SAVE_FAILED(500, "DF002", "불량정보 저장 중 오류가 발생했습니다."),
    INVALID_PROD_RESULT_ID(400, "DF003", "유효하지 않은 생산실적 ID입니다."),

    //생산실적 관련 정보
    PRODUCTION_RESULT_NOT_FOUND(404, "PR001", "생산실적을 찾을 수 없습니다."),
    PRODUCTION_RESULT_ALREADY_COMPLETED(400, "PR002", "이미 생산이 종료된 생산실적입니다."),
    PRODUCTION_QTY_EXCEEDED(400, "PR003", "총 생산 양품수량이 작업지시수량을 초과할 수 없습니다.")
}