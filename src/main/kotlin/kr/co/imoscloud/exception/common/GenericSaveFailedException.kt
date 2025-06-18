package kr.co.imoscloud.exception.common

import kr.co.imoscloud.exception.ErrorCode
import kr.co.imoscloud.exception.ImosException

class GenericSaveFailedException :ImosException(ErrorCode.GENERIC_SAVE_FAILED)