package kr.co.imoscloud.service.sysrtem

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.co.imoscloud.core.UserCacheManager
import kr.co.imoscloud.core.UserRoleCacheManager
import kr.co.imoscloud.dto.*
import kr.co.imoscloud.entity.system.Company
import kr.co.imoscloud.entity.system.QUser.user
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class UserService(
    private val ucm: UserCacheManager,
    private val rcm: UserRoleCacheManager,
    private val jwtProvider: JwtTokenProvider,
    private val codeRep: CodeRep
) : IUser {

    fun signIn(
        loginReq: LoginRequest,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<LoginOutput> {
        val site = getSiteByDomain(request)
        val userRes = ucm.userRepo.findByLoginIdAndFlagActiveIsTrue(loginReq.userId)
            ?.let { user ->
                try {
                    validatePwd(loginReq.userPwd, user)
                    val roleSummery = rcm.getUserRole(user.roleId)
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

                    toUserOutput(user, roleSummery)
                } catch (e: IllegalArgumentException) {
                    // 비밀번호 불일치 관련 로직 Redis 를 이용한 추가 계발 필요
                    toUserOutput()
                }
            }
            ?: throw IllegalArgumentException("유저가 존재하지 않습니다. ")

        return userRes
    }

    @AuthLevel(minLevel = 3)
    fun upsertUser(req: UserInput): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        req.roleId?.let { id -> rcm.validatePriorityIsHigherThan(id, loginUser) }

        val modifyReq = modifyReqByRole(loginUser, req)
        val loginId = req.loginId ?: throw IllegalArgumentException("id is null")

        val upsertUser = ucm.userRepo.findByLoginId(loginId)
            .map { u ->
                rcm.validatePriorityIsHigherThan(u.roleId, loginUser)
                val encodedPwd = pwdEncoder(req.userPwd)
                u.modify(modifyReq, encodedPwd, loginUser)
            }
            .orElseGet { generateUser(req, loginUser) }

        ucm.saveAllAndSyncCache(listOf(upsertUser))
        return upsertUser.let { "${req.loginId} 로그인 성공" }
    }

    /** 본인 정보만 수정 가능한 메서드 */
    @AuthLevel(minLevel = 2)
    fun updateMyInfo(req: UserInput): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        // 본인의 정보만 수정 가능하도록 검증
        if (req.id != loginUser.getId()) {
            throw IllegalArgumentException("본인의 정보만 수정할 수 있습니다.")
        }

        val user = ucm.userRepo.findByIdAndFlagActiveIsTrue(req.id)
            ?.apply {
                loginId = req.loginId ?: this.loginId
                userName = req.userName ?: this.userName
                userEmail = req.userEmail ?: this.userEmail
                phoneNum = req.phoneNum ?: this.phoneNum
                imagePath = req.imagePath ?: this.imagePath
                updateCommonCol(loginUser)
            }
            ?: throw IllegalArgumentException("사용자 정보를 찾을 수 없습니다.")

        ucm.saveAllAndSyncCache(listOf(user))
        return "개인정보 수정이 완료되었습니다."
    }

    fun existLoginId(loginId: String): Boolean = ucm.existsByKey<String, UserSummery?>(loginId)

    fun getUserGroupByCompany(req: UserGroupRequest?): List<UserSummery?> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        val result = if (rcm.isDeveloper(loginUser)) {
            ucm.getUsers(listOf(loginUser.loginId)).filterValues { userGroupFilter(req, it) }.values.mapNotNull { it }
        } else {
            ucm.getAllByCompCd(loginUser).filter { userGroupFilter(req, it) }
        }

        return result
    }

    fun getUserSummery(loginId: String): UserSummery {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        return ifRevisionAllowedThen(loginUser, loginId) { us -> us }
    }

    // id 기반으로 사용자 상세 정보 조회
    fun getUserDetail(loginId: String): UserDetail {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        return ifRevisionAllowedThen(loginUser, loginId) { us: UserSummery ->
            val codeClassIds = listOf("DEPARTMENT", "POSITION")
            val codeMap = codeRep.findAllByCodeClassIdIn(codeClassIds).associate { it?.codeId to it?.codeName }

            val roleSummery = rcm.getUserRole(us.roleId)
                ?: throw IllegalArgumentException("권한 정보를 찾을 수 없습니다. ")

            UserDetail(
                id = us.id,
                site = us.site,
                compCd = us.compCd,
                userName = us.userName ?: "",
                loginId = us.loginId,
                userPwd = us.userPwd,
                imagePath = us.imagePath,
                roleId = us.roleId,
                userEmail = us.userEmail,
                phoneNum = us.phoneNum,
                departmentId = us.departmentId,
                departmentName = codeMap[us.departmentId],
                positionId = us.positionId,
                positionName = codeMap[us.positionId],
                authorityName = roleSummery.roleName,
                flagActive = us.flagActive
            )
        }
    }

    @AuthLevel(minLevel = 3)
    fun deleteUser(loginId: String): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        return ifRevisionAllowedThen(loginUser, loginId) { us: UserSummery ->
            rcm.validatePriorityIsHigherThan(us.roleId, loginUser)
            ucm.softDeleteAndSyncCache(us, loginUser.loginId)
            "$loginId 의 계정 삭제 완료"
        }
    }

    @AuthLevel(minLevel = 3)
    fun resetPassword(loginId: String): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()

        return ifRevisionAllowedThen(loginUser, loginId) { us: UserSummery ->
            rcm.validatePriorityIsHigherThan(us.roleId, loginUser)

            ucm.userRepo.findByLoginIdAndFlagActiveIsTrue(loginId)!!
                .let { u ->
                    val encodedPwd = pwdEncoder("1234")
                    u.apply { userPwd = encodedPwd; updateCommonCol(loginUser) }
                    ucm.saveAllAndSyncCache(listOf(u))
                    "${u.userName} 사용자의 비밀번호 초기화 완료"
                }
        }
    }

    @AuthLevel(minLevel = 2)
    fun changePassword(loginId: String, currentPassword: String, newPassword: String): String {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val encoder = BCryptPasswordEncoder()

        val updateUser = ucm.getUser(loginId)
            ?.let { us -> if (loginUser.loginId == loginId) us else null }
            ?.let {
                ucm.userRepo.findByLoginIdAndFlagActiveIsTrue(loginId)
                    ?.let { u ->
                        if (!encoder.matches(currentPassword, u.userPwd))
                            throw IllegalArgumentException("현재 비밀번호가 일치하지 않습니다."); u
                    }
                    ?.apply {
                        userPwd = encoder.encode(newPassword)
                        updateCommonCol(loginUser)
                    }
                    ?: throw IllegalArgumentException("비밀번호를 변경할 대상이 존재하지 않습니다.")
            }
            ?: throw IllegalArgumentException("비밀번호를 변경할 대상이 존재하지 않습니다.")

        ucm.saveAllAndSyncCache(listOf(updateUser))
        return "${user.userName ?: user.loginId} 사용자의 비밀번호 변경 완료"
    }

    @AuthLevel(minLevel = 5)
    fun generateOwner(company: Company): User {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        val encodedPwd = pwdEncoder("1234")

        return User(
            site = company.site,
            compCd = company.compCd,
            loginId = createLoginId("owner"),
            userPwd = encodedPwd,
            roleId = 2,
        ).apply { createCommonCol(loginUser) }
    }

    private fun pwdEncoder(pwd: String?): String {
        val passwordEncoder = BCryptPasswordEncoder()
        return pwd
            ?.let { passwordEncoder.encode(pwd) }
            ?: throw IllegalArgumentException("패스워드가 비어있습니다 입력해주세요. ")
    }

    private fun validatePwd(matchedPWD: String?, target: User) {
        val passwordEncoder = BCryptPasswordEncoder()
        if (matchedPWD == null) throw NullPointerException("비밀번호를 입력해주세요. ")

        if (!passwordEncoder.matches(matchedPWD, target.userPwd) && target.userPwd != matchedPWD)
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다. ")
    }

    private fun modifyReqByRole(loginUser: UserPrincipal, req: UserInput): UserInput {
        return if (rcm.isDeveloper(loginUser)) {
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
        val encodedPwd = pwdEncoder(req.userPwd)
        val loginId = req.loginId ?: createLoginId()
        return User.create(req, loginId, encodedPwd).apply { createCommonCol(loginUser) }
    }

    private fun userGroupFilter(req: UserGroupRequest?, it: UserSummery?) =
        (req?.roleId?.let { r -> it?.roleId == r } ?: true)
                && (req?.userName?.let { un -> (un.isBlank() || it?.userName == un) } ?: true)
                && (req?.departmentId?.let { dp -> (dp.isBlank() || it?.departmentId == dp) } ?: true)
                && (req?.positionId?.let { p -> (p.isBlank() || it?.positionId == p) } ?: true)


    private fun createLoginId(id: String? = null): String =
        StringBuilder()
            .append(id ?: UUID.randomUUID().toString().replace("-", "").substring(0, 8))
            .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))
            .toString()

    private fun <R> ifRevisionAllowedThen(
        loginUser: UserPrincipal,
        targetLoginId: String,
        onSuccess: (UserSummery) -> R
    ): R {
        return ucm.getUser(targetLoginId)
            ?.let { us: UserSummery ->
                if (!(us.compCd == loginUser.compCd || rcm.isDeveloper(loginUser)))
                    throw IllegalArgumentException("회사 소속이 달라 기능 이용이 제한됩니다. ")

                onSuccess(us)
            }
            ?: throw IllegalArgumentException("대상이 존재하지 않습니다. ")
    }
}