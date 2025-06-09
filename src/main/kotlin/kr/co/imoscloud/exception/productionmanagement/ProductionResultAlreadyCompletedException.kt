package kr.co.imoscloud.exception.productionmanagement

import kr.co.imoscloud.exception.ErrorCode
import kr.co.imoscloud.exception.ImosException

class ProductionResultAlreadyCompletedException: ImosException(ErrorCode.PRODUCTION_RESULT_ALREADY_COMPLETED) 