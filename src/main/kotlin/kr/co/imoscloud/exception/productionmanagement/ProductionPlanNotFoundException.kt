package kr.co.imoscloud.exception.productionmanagement

import kr.co.imoscloud.exception.ErrorCode
import kr.co.imoscloud.exception.ImosException

class ProductionPlanNotFoundException: ImosException(ErrorCode.PRODUCTION_PLAN_NOT_FOUND) 