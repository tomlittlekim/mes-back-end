package kr.co.imoscloud.security

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

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // 1. 요청에서 JWT 토큰 추출
            val jwt = resolveToken(request)

            // 2. JWT 토큰 검증 및 인증 처리
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                val site = getSiteByDomain(request)

                // 인증 객체 생성
                val authentication = jwtTokenProvider.getAuthentication(site, jwt!!)

                // 요청 정보 추가 (선택적)
                if (authentication.details == null) {
                    val details = WebAuthenticationDetailsSource().buildDetails(request)
                    (authentication as org.springframework.security.authentication.AbstractAuthenticationToken).details = details
                }

                // SecurityContext에 인증 객체 설정
                val context = SecurityContextHolder.createEmptyContext()
                context.authentication = authentication
                SecurityContextHolder.setContext(context)

                // 로깅
                log.info("JWT 필터에서 인증 설정 완료: {}", authentication.name)
            } else {
                // 유효한 토큰이 없는 경우
                log.debug("JWT 토큰이 없거나 유효하지 않음")

                // 익명 인증을 방지하기 위해 컨텍스트 초기화
                SecurityContextHolder.clearContext()

                // 401 Unauthorized 응답 반환
                response.status = HttpServletResponse.SC_UNAUTHORIZED
            }
        } catch (e: Exception) {
            log.error("JWT 인증 처리 중 오류 발생", e)
            SecurityContextHolder.clearContext()
            response.status = HttpServletResponse.SC_UNAUTHORIZED
        }

        // 3. 다음 필터 실행
        filterChain.doFilter(request, response)
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