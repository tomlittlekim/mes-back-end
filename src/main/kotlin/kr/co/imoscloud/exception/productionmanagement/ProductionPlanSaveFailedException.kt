package kr.co.imoscloud.exception.productionmanagement

import kr.co.imoscloud.exception.ErrorCode
import kr.co.imoscloud.exception.ImosException

class ProductionPlanSaveFailedException: ImosException(ErrorCode.PRODUCTION_PLAN_SAVE_FAILED) 