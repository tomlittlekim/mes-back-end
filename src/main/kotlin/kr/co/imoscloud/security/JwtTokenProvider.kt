package kr.co.imoscloud.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val userDetailsService: CustomUserDetailsService
) {
    private val logger = LoggerFactory.getLogger(JwtTokenProvider::class.java)

    @Value("\${jwt.secret}")
    private lateinit var secretKey: String

    @Value("\${jwt.token-validity-in-milliseconds}")
    var tokenValidityInMilliseconds: Long = 0

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secretKey.toByteArray(StandardCharsets.UTF_8))
    }

    val ACCESS = "access-token"
    val REFRESH = "refresh_token"

    fun createToken(authentication: Authentication): String {
        val authorities = authentication.authorities.joinToString(",") { it.authority }
        val now = Date()
        val validity = Date(now.time + tokenValidityInMilliseconds)
        val loginId = (authentication.principal as UserPrincipal).getUserId()

        return Jwts.builder()
            .setSubject(loginId)
            .claim("auth", authorities)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(key)
            .compact()
    }

    fun getAuthentication(site: String, token: String): Authentication {
        try {
            val claims = parseClaims(token)

            // 토큰에서 추출한 정보 로깅 (디버깅)
            logger.debug("토큰 페이로드: subject={}, 발행일={}, 만료일={}",
                claims.subject, claims.issuedAt, claims.expiration)

            // 사용자 정보 로드
            val userDetails = userDetailsService.loadUserBySiteAndUserId(site, claims.subject)

            // 사용자 정보가 성공적으로 로드되었는지 확인
            logger.debug("사용자 인증 정보 로드: userId={}, 타입={}",
                claims.subject, userDetails.javaClass.name)

            return UsernamePasswordAuthenticationToken(
                userDetails,  // principal로 UserPrincipal 객체 설정
                "",  // credentials는 비워둠
                userDetails.authorities  // 권한 정보
            )
        } catch (e: Exception) {
            logger.error("인증 객체 생성 중 오류 발생: {}", e.message, e)
            throw e
        }
    }

    fun validateToken(token: String?): Boolean {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token!!)
            return true
        } catch (e: SecurityException) {
            logger.warn("Invalid JWT signature: {}", e.message)
        } catch (e: MalformedJwtException) {
            logger.warn("Invalid JWT token: {}", e.message)
        } catch (e: ExpiredJwtException) {
            logger.warn("Expired JWT token: {}", e.message)
        } catch (e: UnsupportedJwtException) {
            logger.warn("Unsupported JWT token: {}", e.message)
        } catch (e: IllegalArgumentException) {
            logger.warn("JWT claims string is empty: {}", e.message)
        } catch (e: NullPointerException) {
            logger.warn("JWT token is null: {}", e.message)
        }
        return false
    }

    // 토큰에서 클레임 추출
    fun parseClaims(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
    }

    // 디버깅용 토큰 검사 메서드
    fun inspectToken(token: String): Map<String, Any> {
        try {
            val claims = parseClaims(token)
            return mapOf(
                "subject" to (claims.subject ?: ""),
                "issuedAt" to claims.issuedAt.toString(),
                "expiration" to claims.expiration.toString(),
                "claims" to claims.entries.associate { it.key to (it.value?.toString() ?: "") }
            )
        } catch (e: Exception) {
            return mapOf("error" to e.message.toString())
        }
    }
}