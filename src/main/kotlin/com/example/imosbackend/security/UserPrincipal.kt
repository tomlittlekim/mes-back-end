package com.example.imosbackend.security

import lombok.Getter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UserPrincipal(
    private val id: Long,
    private val site: String,
    private val compCd: String,
    private val username: String?,
    private val userId: String,
    private val password: String,
    private val authorities: Collection<GrantedAuthority>
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities

    override fun getPassword(): String = password

    override fun getUsername(): String? = username

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true

    fun getSite(): String = site
    fun getCompCd(): String = compCd
    fun getUserId(): String = userId
    fun getId(): Long = id

    companion object {
        fun create(user: com.example.imosbackend.entity.User): UserPrincipal {
            val authorities = listOf(SimpleGrantedAuthority(user.roleId))

            return UserPrincipal(
                id = user.id,
                site = user.site,
                compCd = user.compCd,
                username = user.userName,
                userId = user.userId,
                password = user.userPwd,
                authorities = authorities
            )
        }
    }
} 