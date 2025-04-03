package kr.co.imoscloud.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.co.imoscloud.core.Core
import kr.co.imoscloud.dto.*
import kr.co.imoscloud.entity.user.User
import kr.co.imoscloud.iface.IUser
import kr.co.imoscloud.repository.CodeRep
import kr.co.imoscloud.security.JwtTokenProvider
import kr.co.imoscloud.security.UserPrincipal
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class UserService(
    private val core: Core,
    private val jwtProvider: JwtTokenProvider,
    private val codeRep: CodeRep
): IUser {

    fun signIn(
        loginReq: LoginRequest,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<LoginOutput> {
        val site = getSiteByDomain(request)
        val userRes = core.userRepo.findBySiteAndLoginIdAndFlagActiveIsTrue(site, loginReq.userId)
            ?.let { user ->
                try {
                    validateUser(loginReq.userPwd, user)
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

    fun signUp(req: UserInput): User {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        if (!core.isAdminOrHigher(loginUser)) throw IllegalArgumentException("관리자 이상의 등급을 가진 유저가 아닙니다. ")

        val modifyReq = modifyReqByRole(loginUser, req)
        val newUser = try {
            val site = modifyReq.site ?: throw IllegalArgumentException("site is null")
            val target = core.userRepo.findBySiteAndLoginIdForSignUp(site, modifyReq.userId)!!
            if (!target.flagActive) target.apply { flagActive = true; createCommonCol(loginUser) }
            else throw IllegalArgumentException("이미 존재하는 유저입니다. ")
        } catch (e: NullPointerException) {
            generateUser(req)
        }

        core.userRepo.save(newUser)
        core.upsertUserFromInMemory(newUser)
        return newUser
    }

    fun existLoginId(req: ExistLoginIdRequest): Boolean {
        val userMap = core.getAllUserMap(listOf(req))
        return userMap[req.loginId] != null
    }

    fun getUserGroupByCompany(): List<UserDetail?> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val codeMap = codeRep.findAllByCodeClassIdIn(listOf("DEPARTMENT","POSITION"))
            .associate { it?.codeId to it?.codeName }

        return if (core.isDeveloper(loginUser)) {
            core.getAllUserMap(listOf(loginUser)).mapValues { userToUserDetail(it.value, codeMap) }.values.toList()
        } else {
            core.getUserGroupByCompCd(loginUser).map { userToUserDetail(it, codeMap) }
        }
    }

    fun getUserDetail(userId: Long): UserDetail {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val target = if (core.isDeveloper(loginUser)) core.userRepo.findByIdAndFlagActiveIsTrue(userId)
        else core.userRepo.findBySiteAndIdAndFlagActiveIsTrue(loginUser.getSite(), userId)

        val codeMap = codeRep.findAllByCodeClassIdIn(listOf("DEPARTMENT","POSITION"))
            .associate { it?.codeId to it?.codeName }
        val roleSummery = core.getUserRoleFromInMemory(loginUser)

        return target
            ?.let{ UserDetail(it.id,it.loginId,it.userName?:"",codeMap[it.departmentId],codeMap[it.positionId],roleSummery.roleName,it.userEmail,it.phoneNum,if(it.flagActive)"Y" else "N") }
            ?:throw UsernameNotFoundException("유저가 존재하지 않습니다. ")
    }

    private fun modifyReqByRole(loginUser: UserPrincipal, req: UserInput): UserInput {
        if (core.isAdminOrHigher(loginUser))
            throw IllegalArgumentException("site 또는 compCd 가 비어있습니다. ")

        return req.apply {
            this.site = loginUser.getSite()
            this.compCd = loginUser.compCd
            if (core.isDeveloper(loginUser)) { site = req.site; compCd = req.compCd }
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

    private fun userToUserDetail(us: UserSummery?, codeMap: Map<String?, String?>): UserDetail? {
        us ?: return null
        val r = core.getUserRoleFromInMemory(us.roleId)
        val departmentNm = codeMap[us.departmentId]
        val positionNm = codeMap[us.positionId]
        val isActive = if(us.flagActive) "Y" else "N"
        return UserDetail(us.id,us.loginId,us.username?:"",departmentNm,positionNm,r.roleName,us.userEmail,us.phoneNum,isActive)
    }
}