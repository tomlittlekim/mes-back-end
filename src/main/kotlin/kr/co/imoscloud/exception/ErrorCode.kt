package kr.co.imoscloud.exception

enum class ErrorCode(
    val status: Int,
    val code: String,
    val message: String
) {

    //유저 관련 정보
    COMPANY_NOT_FOUND(403, "A001", "회사명을 찾을 수 없습니다."),

    //기기 관련 정보
    DEVICE_NOT_FOUND(403,"E001","기기를 찾을 수 없습니다.")
}