package kr.co.imoscloud.dto

import kr.co.imoscloud.exception.ErrorCode
import org.springframework.validation.BindingResult

data class ErrorResponseDto(
    val code: String,
    val message: String,
    val status: Int,
    val errors: List<FieldError> = emptyList()
) {
    companion object {
        fun of(code: ErrorCode, bindingResult: BindingResult): ErrorResponseDto {
            return ErrorResponseDto(
                code = code.code,
                message = code.message,
                status = code.status,
                errors = FieldError.getFieldError(bindingResult)
            )
        }

        fun of(code: ErrorCode): ErrorResponseDto {
            return ErrorResponseDto(
                code = code.code,
                message = code.message,
                status = code.status
            )
        }
    }

    data class FieldError(
        val field: String,
        val value: String?,
        val reason: String?
    ) {
        companion object {
            fun getFieldError(bindingResult: BindingResult): List<FieldError> =
                bindingResult.fieldErrors.map {
                    FieldError(
                        field = it.field,
                        value = it.rejectedValue?.toString(),
                        reason = it.defaultMessage
                    )
                }
        }
    }
}