package kr.co.imoscloud.exception.auth

import kr.co.imoscloud.exception.ErrorCode
import kr.co.imoscloud.exception.ImosException

class CompanyNotFoundException: ImosException(ErrorCode.COMPANY_NOT_FOUND)