package kr.co.imoscloud.security

import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.co.imoscloud.iface.IUser
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter(), IUser {
    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        // /api/login 경로는 필터링 제외
        return request.servletPath.startsWith("/api/login") || request.servletPath.startsWith("/ws/power")
                ||request.servletPath.startsWith("/kpi/devices/data")
    }

    /** 작동 흐름
     * [요청 수신]
     *   → 토큰 추출
     *   → 토큰 존재 여부 확인
     *     │
     *     ├─ 토큰 있음 → 검증 시도
     *     │   ├─ 유효 → 인증 컨텍스트 설정
     *     │   ├─ 만료 → X-Token-Expired 헤더 추가
     *     │   └─ 무효 → 401 반환
     *     │
     *     └─ 토큰 없음 → 401 반환
     * */
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // 1. 요청에서 JWT 토큰 추출
            val jwt = resolveToken(request)

            when {
                StringUtils.hasText(jwt) -> {
                    handleJwtValidation(jwt!!, request, response)
                    if (response.isCommitted) return
                }
                else -> {
                    sendUnauthorizedResponse(response, "JWT 토큰이 없음")
                    return
                }
            }
        } catch (e: Exception) {
            //3. 예외 처리
            handleFilterException(e, response)
            return
        }
        //4. 다음 필터 실행
        filterChain.doFilter(request, response)
    }

    /** 토큰 검증 메서드 */
    private fun handleJwtValidation(jwt: String, request: HttpServletRequest, response: HttpServletResponse) {
        try {
            if (!jwtTokenProvider.validateToken(jwt)) {
                sendUnauthorizedResponse(response, "유효하지 않은 JWT 토큰")
                return
            }
            setAuthenticationContext(jwt, request)
        } catch (ex: ExpiredJwtException) {
            sendTokenExpiredResponse(response)
            return
        }
    }

    /** 인증 컨텍스트 설정 */
    private fun setAuthenticationContext(jwt: String, request: HttpServletRequest) {
        val site = getSiteByDomain(request)

        // 인증 객체 생성
        val authentication = jwtTokenProvider.getAuthentication(site, jwt!!)

        // 요청 정보 추가 (선택적)
        if (authentication.details == null) {
            val details = WebAuthenticationDetailsSource().buildDetails(request)
            (authentication as org.springframework.security.authentication.AbstractAuthenticationToken).details =
                details
        }

        // SecurityContext에 인증 객체 설정
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        SecurityContextHolder.setContext(context)
    }

    private fun handleFilterException(e: Exception, response: HttpServletResponse) {
        log.error("JWT 인증 오류: ${e.message}", e)
        when (e) {
            is ExpiredJwtException -> sendTokenExpiredResponse(response)
            else -> sendUnauthorizedResponse(response, "인증 처리 실패")
        }
    }

    private fun sendTokenExpiredResponse(response: HttpServletResponse) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.setHeader("X-Token-Expired", "true")
        response.setContentType("application/json")

        val jsonResponse = """{"error":"JWT 토큰 만료"}"""
        response.writer.print(jsonResponse)
        response.writer.close()
    }

    /** 401 응답 처리 통합 */
    private fun sendUnauthorizedResponse(response: HttpServletResponse, logMessage: String) {
        log.debug(logMessage)
        SecurityContextHolder.clearContext()

        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.setContentType("application/json")

        val jsonResponse = """{"error":"$logMessage"}"""
        response.writer.print(jsonResponse)
        response.writer.close()
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        // 1. Authorization 헤더에서 토큰 추출
        val bearerToken = request.getHeader("Authorization")
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7)
        }

        // 2. 쿠키에서 토큰 추출
        val cookies = request.cookies ?: return null
        val tokenCookie = cookies.firstOrNull { it.name == jwtTokenProvider.ACCESS }

        return tokenCookie?.value?.let {
            if (it.startsWith("Bearer ")) it.substring(7) else it
        }
    }
}