package kr.co.imoscloud.exception.productionmanagement

import kr.co.imoscloud.exception.ErrorCode
import kr.co.imoscloud.exception.ImosException

class DefectInfoSaveFailedException: ImosException(ErrorCode.DEFECT_INFO_SAVE_FAILED) 