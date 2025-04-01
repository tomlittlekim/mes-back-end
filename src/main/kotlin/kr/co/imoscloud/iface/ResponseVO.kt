package kr.co.imoscloud.iface

import org.springframework.http.ResponseEntity

interface ResponseVO {

    interface ResponseBase {
        val status: Int
        val message: String
    }

    fun <T: ResponseBase> generateResponseEntity(resData: T): ResponseEntity<T> {
        val status = resData.status
        return ResponseEntity.status(status).body(resData)
    }
}