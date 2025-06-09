package kr.co.imoscloud.exception.vendor

import kr.co.imoscloud.exception.ErrorCode
import kr.co.imoscloud.exception.ImosException

class NotExistVendorIdException : ImosException(ErrorCode.NOT_EXIST_VENDOR_ID)