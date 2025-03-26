package kr.co.imoscloud.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException


@Component
@RequiredArgsConstructor
class ExceptionHandlerFilter : OncePerRequestFilter() {
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest, response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            filterChain.doFilter(request, response)
        } catch (e: Exception) {
            // 예외 발생 시 로깅
            logger.error("Exception occurred during filter chain: ${e.message}", e)
            // 필요한 경우 여기에 예외 처리 로직 추가
            // 현재는 예외를 그대로 전파
            throw e
        }
    }

//    @Throws(IOException::class)
//    private fun setExceptionDto(response: HttpServletResponse, exceptionDto: ExceptionDto) {
//        val objectMapper = ObjectMapper()
//        response.contentType = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
//        response.status = exceptionDto.getCode()
//        objectMapper.writeValue(response.writer, exceptionDto)
//    }
}

/*
package kr.co.imoscloud.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException


@Component
@RequiredArgsConstructor
class ExceptionHandlerFilter : OncePerRequestFilter() {
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest, response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val exceptionDto: ExceptionDto

        try {
            filterChain.doFilter(request, response)
        } catch (e: AccessTokenIsExpiredException) {
            exceptionDto = ExceptionDto(401, "RE_ISSUANCE", "")
            setExceptionDto(response, exceptionDto)
        } catch (e: RefreshTokenIsNullException) {
            exceptionDto = ExceptionDto(400, "REFRESH_ISNULL", "Redis 에 refreshToken 이 없습니다 ")
            setExceptionDto(response, exceptionDto)
        } catch (e: OnlyHaveRefreshTokenException) {
            exceptionDto = ExceptionDto(401, "ONLY_HAVE_REFRESH", "")
            setExceptionDto(response, exceptionDto)
        }
    }

    @Throws(IOException::class)
    private fun setExceptionDto(response: HttpServletResponse, exceptionDto: ExceptionDto) {
        val objectMapper = ObjectMapper()
        response.contentType = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
        response.status = exceptionDto.getCode()
        objectMapper.writeValue(response.writer, exceptionDto)
    }
}
*/