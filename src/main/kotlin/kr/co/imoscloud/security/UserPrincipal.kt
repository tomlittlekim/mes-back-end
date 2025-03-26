package kr.co.imoscloud.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UserPrincipal(
    private val id: Long,
    private val username: String,
    private val userId: String,
    private val password: String,
    private val authorities: Collection<GrantedAuthority>
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities

    override fun getPassword(): String = password

    override fun getUsername(): String = username

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true

    fun getUserId(): String = userId

    companion object {
        fun create(user: kr.co.imoscloud.entity.User): UserPrincipal {
            val authorities = listOf(SimpleGrantedAuthority(user.roleId))

            return UserPrincipal(
                id = user.id,
                username = user.userName,
                userId = user.userId,
                password = user.userPwd,
                authorities = authorities
            )
        }
    }
} 