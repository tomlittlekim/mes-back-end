package kr.co.imoscloud.exception.company

import kr.co.imoscloud.exception.ErrorCode
import kr.co.imoscloud.exception.ImosException

class CompanyNotFoundException: ImosException(ErrorCode.COMPANY_NOT_FOUND)