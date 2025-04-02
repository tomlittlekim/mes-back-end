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
                if (log.isDebugEnabled) {
                    log.debug("인증 Principal 타입: {}",
                        if (authentication.principal != null) authentication.principal.javaClass.name else "null")
                    log.debug("SecurityContext에 인증 정보 설정됨: {}",
                        SecurityContextHolder.getContext().authentication != null)
                }
            } else {
                // 유효한 토큰이 없는 경우
                log.debug("JWT 토큰이 없거나 유효하지 않음")
                
                // 익명 인증을 방지하기 위해 컨텍스트 초기화
                SecurityContextHolder.clearContext()
                
                // 401 Unauthorized 응답 반환
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                response.writer.write("{\"error\":\"Unauthorized\",\"message\":\"유효한 인증 토큰이 필요합니다.\"}")
            }
        } catch (e: Exception) {
            log.error("JWT 인증 처리 중 오류 발생", e)
            SecurityContextHolder.clearContext()
        }

        // 3. 다음 필터 실행
        filterChain.doFilter(request, response)

        // 4. 필터 체인 실행 후 SecurityContext 확인 (디버깅용)
        if (log.isDebugEnabled) {
            val authAfterChain = SecurityContextHolder.getContext().authentication
            if (authAfterChain != null) {
                log.debug("필터 체인 이후 인증 타입: {}", authAfterChain.javaClass.name)
                log.debug("필터 체인 이후 Principal 타입: {}",
                    if (authAfterChain.principal != null) authAfterChain.principal.javaClass.name else "null")
            } else {
                log.debug("필터 체인 이후 인증 정보 없음")
            }
        }
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        // 쿠키에서 토큰 추출
        val cookies = request.cookies ?: return null

        val tokenCookie = cookies.firstOrNull { it.name == jwtTokenProvider.ACCESS }
        val tokenValue = tokenCookie?.value ?: return null

        if (log.isDebugEnabled) {
            val tokenDisplay = if (tokenValue.length > 15)
                tokenValue.substring(0, 15) + "..."
            else tokenValue
            log.debug("쿠키에서 토큰 추출: {}", tokenDisplay)
        }

        // Bearer 접두사 제거
        return if (tokenValue.startsWith("Bearer ")) tokenValue.substring(7) else tokenValue
    }
}