package kr.co.imoscloud.iface

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import kr.co.imoscloud.dto.LoginOutput
import kr.co.imoscloud.dto.RoleSummery
import kr.co.imoscloud.entity.system.User
import org.springframework.http.ResponseEntity

interface IUser: ResponseVO {
    companion object {
        val IMOS = "imos"
        val PEMS = "pems"
    }

    fun getSiteByDomain(req: HttpServletRequest): String {
        val rawRequest = (req as HttpServletRequestWrapper).request as HttpServletRequest
        val domain = rawRequest.serverName
        return when (domain) {
            "imos-cloud.co.kr", "localhost" -> IMOS
            "pems-cloud.co.kr" -> PEMS
            else -> throw IllegalArgumentException("지원하는 도멘인이 아닙니다. ")
        }
    }

    fun toUserOutput(user: User?=null, role: RoleSummery?=null): ResponseEntity<LoginOutput> {
        val output = user
            ?.let { LoginOutput(
                id = user.id,
                site = user.site,
                compCd = user.compCd,
                loginId = user.loginId,
                userNm = user.userName,
                email = user.userEmail,
                roleId = user.roleId,
                roleNm = role?.roleName,
                priorityLevel = role?.priorityLevel,
                status = 200,
                message = "${user.loginId} 로그인 성공"
            )}
            ?:run { LoginOutput(
                status = 404,
                message = "로그인 실패"
            )}

        return generateResponseEntity(output)
    }
}