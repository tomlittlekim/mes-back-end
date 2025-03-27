package kr.co.imoscloud.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.co.imoscloud.entity.User
import kr.co.imoscloud.fetcher.UserFetcher.UserInput
import kr.co.imoscloud.repository.UserRepository
import kr.co.imoscloud.security.JwtTokenProvider
import kr.co.imoscloud.security.UserPrincipal
import org.springframework.http.ResponseCookie
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class UserService(
    private val userRepo: UserRepository,
    private val jwtProvider: JwtTokenProvider
) {

    fun signIn(req: UserInput, servletRequest: HttpServletRequest, servletResponse: HttpServletResponse): String {
        val domainNm = servletRequest.serverName
        val targetId = req.userId ?: throw IllegalStateException("UserId를 입력해주세요")
        return userRepo.findBySiteAndUserIdAndIsActiveIsTrue(domainNm, targetId)
            ?.let { user ->
                try {
                    validateUser(req.password, user)
                    val userDetails = UserPrincipal.create(user)
                    val userPrincipal = UsernamePasswordAuthenticationToken(userDetails, "", userDetails.authorities)
                    val token = jwtProvider.createToken(userPrincipal)
                    val cookie = ResponseCookie.from(jwtProvider.ACCESS, token)
                        .path("/")
                        .maxAge(jwtProvider.tokenValidityInMilliseconds)
                        .httpOnly(true)
                        .build()
                    servletResponse.addHeader("Set-Cookie", cookie.toString())
                    "${user.userId} 로그인 성공"
                } catch (e: IllegalArgumentException) {
                    // 비밀번호 불일치 관련 로직 Redis 를 이용한 추가 계발 필요
                    "로그인 실패"
                }
            }
            ?:throw IllegalArgumentException("유저가 존재하지 않습니다. ")
    }

    fun signUp(req: UserInput, loginUser: UserPrincipal): User {
        if (!checkRole(loginUser)) throw IllegalArgumentException("관리자 이상의 등급을 가진 유저가 아닙니다. ")

        val modifyReq = modifyReqByRole(loginUser, req)
        val newUser = try {
            val target = userRepo.findBySiteAndUserIdForSignUp(modifyReq.site!!, modifyReq.userId)!!
            if (target.isActive == false) target.apply { isActive = true; createCommonCol(loginUser) }
            else throw IllegalArgumentException("이미 존재하는 유저입니다. ")
        } catch (e: NullPointerException) {
            generateUser(req)
        }

        return userRepo.save(newUser)
    }

    private fun checkRole(loginUser: UserPrincipal): Boolean {
        return loginUser.authorities.first().authority == "admin"
    }

    private fun modifyReqByRole(loginUser: UserPrincipal, req: UserInput): UserInput {
        val isDev = loginUser.authorities.first().authority == "dev"
        if (isDev && req.site == null || isDev && req.compCd == null)
            throw IllegalArgumentException("site 또는 compCd 가 비어있습니다. ")

        return req.apply {
            this.site = if (isDev) req.site else loginUser.getSite()
            this.compCd = if (isDev) req.compCd else loginUser.getCompCd()
        }
    }

    private fun generateUser(req: UserInput): User {
        val uuid =  UUID.randomUUID().toString()
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        return User(
            site = req.site!!,
            compCd = req.compCd!!,
            userId = req.userId ?: (uuid.substring(0, 7) + formatter.format(today)),
            userPwd = uuid.substring(7, 14) +"!@",
            roleId = req.roleId
        )
    }

    private fun validateUser(matchedPWD: String?, target: User) {
        val passwordEncoder = BCryptPasswordEncoder()
        if (matchedPWD == null) throw NullPointerException("비밀번호를 입력해주세요. ")

        if (!passwordEncoder.matches(target.userPwd, matchedPWD))
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다. ")
    }
}