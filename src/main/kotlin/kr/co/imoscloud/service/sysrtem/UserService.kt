package kr.co.imoscloud.service.sysrtem

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.co.imoscloud.core.Core
import kr.co.imoscloud.dto.*
import kr.co.imoscloud.entity.system.Company
import kr.co.imoscloud.entity.system.User
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
import java.time.LocalDateTime
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
                    val roleSummery = core.getUserRoleFromInMemory(user.roleId)
                        ?: throw IllegalArgumentException("권한 정보를 찾을 수 없습니다. ")

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
        req.roleId?.let { id -> core.validatePriorityIsHigherThan(id, loginUser) }

        val modifyReq = modifyReqByRole(loginUser, req)
        val loginId = req.loginId ?: throw IllegalArgumentException("id is null")

        val upsertUser = core.getUserFromInMemory(loginId)
            ?.let { user ->
                core.validatePriorityIsHigherThan(user.roleId, loginUser)

                val site = modifyReq.site ?: throw IllegalArgumentException("site is null")
                val target = core.userRepo.findBySiteAndLoginIdForSignUp(site, user.loginId)!!
                modifyUser(target, req, loginUser)
            }
            ?:run { generateUser(req, loginUser) }

        core.userRepo.save(upsertUser)
        core.upsertFromInMemory(upsertUser)
        return upsertUser.let { "${req.loginId} 로그인 성공" }
    }

    fun existLoginId(req: ExistLoginIdRequest): Boolean = core.getUserFromInMemory(req.loginId) != null

    fun getUserGroupByCompany(req: UserGroupRequest?): List<UserSummery?> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        return if (core.isDeveloper(loginUser)) {
            core.getAllUserMap(loginUser).filterValues { userGroupFilter(req, it) }.values.mapNotNull { it }
        } else {
            core.getUserGroupByCompCd(loginUser).filter { userGroupFilter(req, it) }
        }
    }

    fun getUserSummery(loginId: String): UserSummery {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val target = core.getUserFromInMemory(loginId)
        if (!core.isDeveloper(loginUser) && target?.compCd != loginUser.compCd)
            throw IllegalArgumentException("소속이 달라서 조회할 수 없습니다. ")

        return target ?: throw IllegalArgumentException("조회 하려는 유저의 정보가 존재하지 않습니다. ")
    }

    // 업테, 종목, 권한이 id 값이 아닌 name으로 맵핑되어 반환
    fun getUserDetail(loginId: String): UserDetail {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val target = core.getUserFromInMemory(loginId)
        if (!core.isDeveloper(loginUser) && target?.compCd != loginUser.compCd)
            throw IllegalArgumentException("소속이 달라서 조회할 수 없습니다. ")

        val codeClassIds = listOf("DEPARTMENT","POSITION")
        val codeMap = codeRep.findAllByCodeClassIdIn(codeClassIds).associate { it?.codeId to it?.codeName }

        val roleSummery = core.getUserRoleFromInMemory(loginUser.roleId ?: 0)
            ?: throw IllegalArgumentException("권한 정보를 찾을 수 없습니다. ")

        return target?.let { 
            UserDetail(
                id = it.id,
                site = it.site,
                compCd = it.compCd,
                userName = it.userName ?: "",
                loginId = it.loginId,
                userPwd = it.userPwd,
                imagePath = it.imagePath,
                roleId = it.roleId,
                userEmail = it.userEmail,
                phoneNum = it.phoneNum,
                departmentId = it.departmentId,
                departmentName = codeMap[it.departmentId],
                positionId = it.positionId,
                positionName = codeMap[it.positionId],
                authorityName = roleSummery.roleName,
                flagActive = it.flagActive
            )
        } ?: throw UsernameNotFoundException("유저가 존재하지 않습니다. ")
    }

    @AuthLevel(minLevel = 3)
    fun deleteUser(id: Long): String {
        return core.userRepo.findById(id).map { u ->
            val loginUser = SecurityUtils.getCurrentUserPrincipal()
            core.validatePriorityIsHigherThan(u.roleId, loginUser)
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
                core.validatePriorityIsHigherThan(user.roleId, loginUser)

                // company 객체 내부에 초기화 비밀번호 값을 가지고 있거나 \\ 사용자가 입력 하는 방식으로 진행해야함
                val encoder = BCryptPasswordEncoder()
                user.apply { userPwd = encoder.encode("1234"); updateCommonCol(loginUser) }
                core.userRepo.save(user)
                core.upsertFromInMemory(user)
                "${user.userName} 사용자의 비밀번호 초기화 완료"
            }
            .orElseThrow { throw IllegalArgumentException("비밀번호를 초기화할 대상이 존재하지 않습니다. ") }
    }

    @AuthLevel(minLevel = 5)
    fun generateOwner(company: Company): User {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val encoder = BCryptPasswordEncoder()

        return User(
            site = company.site,
            compCd = company.compCd,
            loginId = createLoginId("owner"),
            userPwd = encoder.encode("1234"),
            roleId = 2,
        ).apply { createCommonCol(loginUser) }
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
        val passwordEncoder = BCryptPasswordEncoder()

        return User(
            site = req.site!!,
            compCd = req.compCd!!,
            loginId = req.loginId ?: createLoginId(),
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

        if (!passwordEncoder.matches(matchedPWD, target.userPwd) && target.userPwd != matchedPWD)
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다. ")
    }

//    private fun userToUserDetail(us: UserSummery?, codeMap: Map<String?, String?>): UserDetail? {
//        us ?: return null
//        val r = core.getUserRoleFromInMemory(us.roleId) ?: throw IllegalArgumentException("권한 정보를 찾을 수 없습니다. ")
//        val departmentNm = codeMap[us.departmentId]
//        val positionNm = codeMap[us.positionId]
//        val isActive = if(us.flagActive) "Y" else "N"
//        return UserDetail(us.id,us.loginId,us.userName?:"",departmentNm!! ,positionNm!! ,r.roleName, us.userEmail, us.phoneNum,isActive)
//    }

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

    private fun createLoginId(id: String?=null): String =
        StringBuilder()
            .append(id ?: UUID.randomUUID().toString().replace("-","").substring(0,8))
            .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))
            .toString()
}