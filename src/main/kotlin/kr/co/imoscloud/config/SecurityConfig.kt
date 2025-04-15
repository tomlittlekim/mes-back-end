package kr.co.imoscloud.config

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletResponse
import kr.co.imoscloud.security.ExceptionHandlerFilter
import kr.co.imoscloud.security.JwtAuthenticationFilter
import kr.co.imoscloud.security.JwtTokenProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtTokenProvider: JwtTokenProvider
) {
    // SecurityContextHolder 전략 설정
    @PostConstruct
    fun enableAuthenticationContextOnSpawnedThreads() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        // JwtAuthenticationFilter 인스턴스 생성
        val jwtFilter = JwtAuthenticationFilter(jwtTokenProvider)
        val exceptionFilter = ExceptionHandlerFilter()

        http
            // CSRF 보호 비활성화 (JWT 사용 시 일반적)
            .csrf { it.disable() }

            // CORS 설정
            .cors { it.configurationSource(corsConfigurationSource()) }

            // 세션 관리 설정 (JWT 사용으로 stateless)
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

            // URL 별 접근 권한 설정
            .authorizeHttpRequests { auth ->
                auth
                    // 공개 엔드포인트 설정
                    .requestMatchers("/api/auth/**", "/api/login", "/api/register").permitAll()
                    // GraphQL 엔드포인트는 인증 필요
                    .requestMatchers("/graphql").authenticated()
                    // 그 외 모든 요청은 인증 필요
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint { request, response, authException ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")
                }
                it.accessDeniedHandler { request, response, accessDeniedException ->
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden")
                }
            }
            // 중요: 필터 순서 설정 (JwtFilter가 AnonymousAuthenticationFilter보다 먼저 실행)
            .addFilterBefore(jwtFilter, AnonymousAuthenticationFilter::class.java)
            // 예외 처리 필터 등록
            .addFilterBefore(exceptionFilter, JwtAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf("http://localhost:3000", "http://imos-cloud.co.kr", "http://pems-cloud.co.kr")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            exposedHeaders = listOf("X-Token-Expired")
            allowCredentials = true
        }
        
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}