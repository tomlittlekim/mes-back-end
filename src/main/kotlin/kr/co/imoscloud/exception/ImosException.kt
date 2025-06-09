package kr.co.imoscloud.exception

abstract class ImosException(
    val errorCode: ErrorCode
) : RuntimeException(errorCode.message)