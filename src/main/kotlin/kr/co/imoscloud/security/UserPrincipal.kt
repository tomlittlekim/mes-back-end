package kr.co.imoscloud.security

import kr.co.imoscloud.dto.RoleSummery
import kr.co.imoscloud.dto.UserSummery
import kr.co.imoscloud.entity.user.User
import kr.co.imoscloud.iface.DtoAllInOneBase
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UserPrincipal(
    private val id: Long,
    private val site: String,
    override val compCd: String,
    private val username: String?,
    override val loginId: String,
    private val password: String,
    override val roleId: Long,
    private val authorities: Collection<GrantedAuthority>
) : UserDetails, DtoAllInOneBase {

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities

    override fun getPassword(): String = password

    override fun getUsername(): String? = username

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true

    fun getSite(): String = site
    fun getCompCd(): String = compCd
    fun getLoginId(): String = loginId
    fun getId(): Long = id
    fun getRoleId(): Long = roleId

    companion object {
        fun create(user: User, role: RoleSummery): UserPrincipal {
            val authorities = listOf(SimpleGrantedAuthority(role.roleName))

            return UserPrincipal(
                id = user.id,
                site = user.site,
                compCd = user.compCd,
                username = user.userName,
                loginId = user.loginId,
                password = user.userPwd,
                roleId = user.roleId,
                authorities = authorities
            )
        }

        fun create(user: UserSummery, role: RoleSummery): UserPrincipal {
            val authorities = listOf(SimpleGrantedAuthority(role.roleName))

            return UserPrincipal(
                id = user.id,
                site = user.site,
                compCd = user.compCd,
                username = user.username,
                loginId = user.loginId,
                password = user.userPwd,
                roleId = user.roleId,
                authorities = authorities
            )
        }
    }
} 