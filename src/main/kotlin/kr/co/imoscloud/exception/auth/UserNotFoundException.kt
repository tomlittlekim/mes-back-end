package kr.co.imoscloud.exception.auth

import kr.co.imoscloud.exception.ErrorCode
import kr.co.imoscloud.exception.ImosException

class UserNotFoundException: ImosException(ErrorCode.USER_NOT_FOUND) 