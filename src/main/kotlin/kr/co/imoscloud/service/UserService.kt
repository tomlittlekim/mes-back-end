package kr.co.imoscloud.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.co.imoscloud.core.Core
import kr.co.imoscloud.dto.LoginRequest
import kr.co.imoscloud.dto.RoleInput
import kr.co.imoscloud.dto.UserInput
import kr.co.imoscloud.dto.UserOutput
import kr.co.imoscloud.entity.user.User
import kr.co.imoscloud.iface.IUser
import kr.co.imoscloud.repository.user.UserRepository
import kr.co.imoscloud.security.JwtTokenProvider
import kr.co.imoscloud.security.UserPrincipal
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class UserService(
    private val userRepo: UserRepository,
    private val jwtProvider: JwtTokenProvider,
    private val core: Core
): IUser {

    fun signIn(
        loginReq: LoginRequest,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<UserOutput> {
        val site = getSiteByDomain(request)
        val userRes = userRepo.findBySiteAndLoginIdAndFlagActiveIsTrue(site, loginReq.userId)
            ?.let { user ->
                try {
                    validateUser(loginReq.userPwd, user)

                    val testIndies = (1..5).map { RoleInput(it.toLong()) }
                    val roleMap = core.getAllRoleMap(testIndies)

//                    val testOutPutReq = (1..5).map { UserOutput(
//                        status = 200,
//                        message = "test",
//                        userId = it.toLong(),
//                        roleId = it.toLong()
//                    ) }
//                    val resultMap = core.extractReferenceDataMaps(testOutPutReq)


                    val roleSummery = core.getUserRoleFromInMemory(user)

                    val userDetails = UserPrincipal.create(user, roleSummery)
                    val userPrincipal = UsernamePasswordAuthenticationToken(userDetails, "", userDetails.authorities)
                    val token = jwtProvider.createToken(userPrincipal)

                    val cookie = ResponseCookie.from(jwtProvider.ACCESS, token)
                        .path("/")
                        .maxAge(jwtProvider.tokenValidityInMilliseconds)
                        .httpOnly(true)
                        .build()
                    response.addHeader("Set-Cookie", cookie.toString())

                    userToUserOutput(user, roleSummery)
                } catch (e: IllegalArgumentException) {
                    // 비밀번호 불일치 관련 로직 Redis 를 이용한 추가 계발 필요
                    userToUserOutput()
                }
            }
            ?:throw IllegalArgumentException("유저가 존재하지 않습니다. ")

        return userRes
    }

    fun signUp(req: UserInput, loginUser: UserPrincipal): User {
        if (!checkRole(loginUser)) throw IllegalArgumentException("관리자 이상의 등급을 가진 유저가 아닙니다. ")

        val modifyReq = modifyReqByRole(loginUser, req)
        val newUser = try {
            val target = userRepo.findBySiteAndLoginIdForSignUp(modifyReq.site!!, modifyReq.userId)!!
            if (target.flagActive == false) target.apply { flagActive = true; createCommonCol(loginUser) }
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

        val passwordEncoder = BCryptPasswordEncoder()
        val pwd = uuid.substring(7, 14) +"!@"

        return User(
            site = req.site!!,
            compCd = req.compCd!!,
            loginId = req.userId ?: (uuid.substring(0, 7) + formatter.format(today)),
            userPwd = passwordEncoder.encode(pwd),
            roleId = req.roleId!!
        )
    }

    private fun validateUser(matchedPWD: String?, target: User) {
        val passwordEncoder = BCryptPasswordEncoder()
        if (matchedPWD == null) throw NullPointerException("비밀번호를 입력해주세요. ")

        if (!(passwordEncoder.matches(target.userPwd, matchedPWD) || target.userPwd == matchedPWD))
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다. ")
    }
}