package kr.co.imoscloud.exception.productionmanagement

import kr.co.imoscloud.exception.ErrorCode
import kr.co.imoscloud.exception.ImosException

class WorkOrderSaveFailedException: ImosException(ErrorCode.WORK_ORDER_SAVE_FAILED) 