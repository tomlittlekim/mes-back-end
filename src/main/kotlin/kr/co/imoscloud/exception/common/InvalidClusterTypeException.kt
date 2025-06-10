package kr.co.imoscloud.exception.common

import kr.co.imoscloud.exception.ErrorCode
import kr.co.imoscloud.exception.ImosException

class InvalidClusterTypeException: ImosException(ErrorCode.INVALID_CLUSTER_TYPE)