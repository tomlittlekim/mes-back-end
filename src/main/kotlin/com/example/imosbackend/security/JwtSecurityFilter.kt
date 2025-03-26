package com.example.imosbackend.security

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException


@RequiredArgsConstructor
class JwtSecurityFilter : OncePerRequestFilter() {

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest, httpServletResponse: HttpServletResponse,
        filterChain: FilterChain
    ) {
        filterChain.doFilter(request, httpServletResponse)
    }
}