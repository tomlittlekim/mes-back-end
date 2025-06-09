package kr.co.imoscloud.exception.productionmanagement

import kr.co.imoscloud.exception.ErrorCode
import kr.co.imoscloud.exception.ImosException

class WorkOrderNotFoundException: ImosException(ErrorCode.WORK_ORDER_NOT_FOUND) 