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
    NOT_EXIST_BUSINESS_REG_NO(400,"V003","사업자 번호가 존재하지 않습니다.")
}