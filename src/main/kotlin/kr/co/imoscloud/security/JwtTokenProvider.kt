package kr.co.imoscloud.security

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
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

    private lateinit var key: SecretKey
    val ACCESS = "access-token"
    val REFRESH = "refresh_token"

    fun init() {
        key = Keys.hmacShaKeyFor(secretKey.toByteArray())
    }

    fun createToken(authentication: Authentication): String {
        val authorities = authentication.authorities.joinToString(",") { it.authority }
        val now = Date()
        val validity = Date(now.time + tokenValidityInMilliseconds)

        return Jwts.builder()
            .setSubject(authentication.name)
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