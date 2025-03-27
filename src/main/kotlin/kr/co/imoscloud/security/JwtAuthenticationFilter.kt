package kr.co.imoscloud.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val jwt = resolveToken(request)
        
        if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
            val domainNm = request.serverName
            val authentication = jwtTokenProvider.getAuthentication(domainNm, jwt!!)
            SecurityContextHolder.getContext().authentication = authentication
        }
        
        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val cookies = request.cookies ?: return null

        val tokenCookie = cookies.firstOrNull { it.name == jwtTokenProvider.ACCESS }
        val tokenValue = tokenCookie?.value ?: return null

        return if (tokenValue.startsWith("Bearer ")) tokenValue.substring(7) else tokenValue
    }
} 