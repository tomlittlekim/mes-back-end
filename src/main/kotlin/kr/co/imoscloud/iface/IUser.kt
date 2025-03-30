package kr.co.imoscloud.iface

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import kr.co.imoscloud.entity.user.User
import kr.co.imoscloud.fetcher.UserFetcher
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

    fun userToUserOutput(user: User?): ResponseEntity<UserFetcher.UserOutput> {
        val output = user
            ?.let { UserFetcher.UserOutput(
                userId = user.userId,
                userNm = user.userName,
                email = user.userEmail,
                roleId = user.roleId,
                status = 200,
                message = "${user.userId} 로그인 성공"
            )}
            ?:run { UserFetcher.UserOutput(
                status = 200,
                message = "로그인 실패"
            )}

        return generateResponseEntity(output)
    }
}