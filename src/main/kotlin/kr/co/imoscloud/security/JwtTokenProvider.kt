package kr.co.imoscloud.security

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
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
        val userId = (authentication.principal as UserPrincipal).getUserId()

        return Jwts.builder()
            .setSubject(userId)
            .claim("auth", authorities)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(key)
            .compact()
    }

    fun getAuthentication(site: String, token: String): Authentication {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body

        val userDetails = userDetailsService.loadUserBySiteAndUserId(site, claims.subject)
        return UsernamePasswordAuthenticationToken(userDetails, "", userDetails.authorities)
    }

    fun validateToken(token: String?): Boolean {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token!!)
            return true
        } catch (e: SecurityException) {
            println("Invalid JWT signature.")
        } catch (e: MalformedJwtException) {
            println("Invalid JWT token.")
        } catch (e: ExpiredJwtException) {
            println("Expired JWT token.")
        } catch (e: UnsupportedJwtException) {
            println("Unsupported JWT token.")
        } catch (e: IllegalArgumentException) {
            println("JWT claims string is empty.")
        } catch (e: NullPointerException) {
            println("JWT claims string is empty.")
        }
        return false
    }
} 