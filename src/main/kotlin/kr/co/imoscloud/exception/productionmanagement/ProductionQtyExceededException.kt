package kr.co.imoscloud.exception.productionmanagement

import kr.co.imoscloud.exception.ErrorCode
import kr.co.imoscloud.exception.ImosException

class ProductionQtyExceededException: ImosException(ErrorCode.PRODUCTION_QTY_EXCEEDED) 