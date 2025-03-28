package kr.co.imoscloud.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.co.imoscloud.iface.IUser
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter(), IUser {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val jwt = resolveToken(request)

        if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
            val site = getSiteByDomain(request)
            val authentication = jwtTokenProvider.getAuthentication(site, jwt!!)
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