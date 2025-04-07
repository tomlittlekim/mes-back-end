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
import kr.co.imoscloud.util.AuthLevel
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
        val userRes = core.userRepo.findByLoginIdAndFlagActiveIsTrue(loginReq.userId)
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

    @AuthLevel(minLevel = 3)
    fun upsertUser(req: UserInput): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val modifyReq = modifyReqByRole(loginUser, req)

        val upsertUser = core.getUserFromInMemory(req.loginId)
            ?.let { user ->
                val site = modifyReq.site ?: throw IllegalArgumentException("site is null")
                val target = core.userRepo.findBySiteAndLoginIdForSignUp(site, user.loginId)!!
                modifyUser(target, req, loginUser)
            }
            ?:run { generateUser(req, loginUser) }

        core.userRepo.save(upsertUser)
        core.upsertFromInMemory(upsertUser)
        return upsertUser.let { "${req.loginId} 로그인 성공" }
    }

    fun existLoginId(req: ExistLoginIdRequest): Boolean = core.getUserFromInMemory(req) != null

    fun getUserGroupByCompany(req: UserGroupRequest?): List<UserSummery?> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        return if (core.isDeveloper(loginUser)) {
            core.getAllUserMap(listOf(loginUser)).filterValues { userGroupFilter(req, it) }.values.mapNotNull { it }
        } else {
            core.getUserGroupByCompCd(loginUser).filter { userGroupFilter(req, it) }
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

    @AuthLevel(minLevel = 3)
    fun deleteUser(id: Long): String {
        return core.userRepo.findById(id).map { u ->
            val loginUser = SecurityUtils.getCurrentUserPrincipal()
            core.validatePriorityIsHigherThan(u, loginUser)
            core.userRepo.delete(u)
            core.deleteFromInMemory(u)
            "${u.loginId} 의 계정 삭제 완료"
        }.orElseThrow { throw IllegalArgumentException("삭제 대상의 정보가 존재하지 않습니다. ") }
    }

    @AuthLevel(minLevel = 3)
    fun resetPassword(id: Long): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        return core.userRepo.findById(id)
            .map { user ->
                core.validatePriorityIsHigherThan(user, loginUser)
                // company 객체 내부에 초기화 비밀번호 값을 가지고 있거나 \\ 사용자가 입력 하는 방식으로 진행해야함
                val encoder = BCryptPasswordEncoder()
                user.apply { userPwd = encoder.encode("1234"); updateCommonCol(loginUser) }
                core.userRepo.save(user)
                core.upsertFromInMemory(user)
                "${user.userName} 사용자의 비밀번호 초기화 완료"
            }
            .orElseThrow { throw IllegalArgumentException("비밀번호를 초기화할 대상이 존재하지 않습니다. ") }
    }

    private fun modifyReqByRole(loginUser: UserPrincipal, req: UserInput): UserInput {
        return if (core.isDeveloper(loginUser)) {
            req.site ?: throw IllegalArgumentException("site 가 존재하지 않습니다. ")
            req.compCd ?: throw IllegalArgumentException("compCd 가 존재하지 않습니다. ")
            req
        } else {
            req.apply {
                this.site = loginUser.getSite()
                this.compCd = loginUser.compCd
            }
        }
    }

    private fun generateUser(req: UserInput, loginUser: UserPrincipal): User {
        val uuid =  UUID.randomUUID().toString()
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        val passwordEncoder = BCryptPasswordEncoder()

        return User(
            site = req.site!!,
            compCd = req.compCd!!,
            loginId = req.loginId ?: (uuid.substring(0, 7) + formatter.format(today)),
            userPwd = passwordEncoder.encode(req.userPwd),
            userName = req.userName,
            userEmail = req.userEmail,
            roleId = req.roleId!!,
            phoneNum = req.phoneNum,
            departmentId = req.departmentId!!,
            positionId = req.positionId!!,
        ).apply { createCommonCol(loginUser) }
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
        return UserDetail(us.id,us.loginId,us.userName?:"",departmentNm,positionNm,r.roleName,us.userEmail,us.phoneNum,isActive)
    }

    private fun userGroupFilter(req: UserGroupRequest?, it: UserSummery?) =
        (req?.roleId?.let { r ->  it?.roleId == r } ?: true)
        && (req?.userName?.let { un -> (un.isBlank() || it?.userName == un) } ?: true)
        && (req?.departmentId?.let { dp -> (dp.isBlank() || it?.departmentId == dp) } ?: true)
        && (req?.positionId?.let { p -> (p.isBlank() || it?.positionId == p) } ?: true)

    private fun modifyUser(target: User, req: UserInput, loginUser: UserPrincipal): User {
        return target.apply {
            loginId = req.loginId ?: this.loginId
            userPwd = req.userPwd?.let {
                val passwordEncoder = BCryptPasswordEncoder()
                passwordEncoder.encode(req.userPwd)
            } ?: this.userPwd
            userName = req.userName ?: this.userName
            userEmail = req.userEmail ?: this.userEmail
            //imagePath
            roleId = req.roleId ?: this.roleId
            phoneNum = req.phoneNum ?: this.phoneNum
            departmentId = req.departmentId ?: this.departmentId
            positionId = req.positionId ?: this.positionId
            flagActive = req.flagActive ?: true
            updateCommonCol(loginUser)
        }
    }
}