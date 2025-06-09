package kr.co.imoscloud.exception.equipment

import kr.co.imoscloud.exception.ErrorCode
import kr.co.imoscloud.exception.ImosException

class DeviceNotFoundException: ImosException(ErrorCode.DEVICE_NOT_FOUND)