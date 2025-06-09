package kr.co.imoscloud.exception.productionmanagement

import kr.co.imoscloud.exception.ErrorCode
import kr.co.imoscloud.exception.ImosException

class ProductionResultNotFoundException: ImosException(ErrorCode.PRODUCTION_RESULT_NOT_FOUND) 