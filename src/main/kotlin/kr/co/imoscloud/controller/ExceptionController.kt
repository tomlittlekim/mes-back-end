package kr.co.imoscloud.controller

import kr.co.imoscloud.dto.ErrorResponseDto
import kr.co.imoscloud.exception.ErrorCode
import kr.co.imoscloud.exception.ImosException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice


@RestControllerAdvice
class ExceptionController {

    @ExceptionHandler(ImosException::class)
    fun imosException(e: ImosException): ResponseEntity<ErrorResponseDto> {
        val errorCode = e.errorCode
        val errorResponseDto: ErrorResponseDto = ErrorResponseDto.of(errorCode)

        return ResponseEntity.status(errorCode.status).body(errorResponseDto)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(e: DataIntegrityViolationException): ResponseEntity<ErrorResponseDto> {
        val dto = ErrorResponseDto.of(ErrorCode.GENERIC_SAVE_FAILED)

        return ResponseEntity.status(ErrorCode.GENERIC_SAVE_FAILED.status).body(dto)
    }

}