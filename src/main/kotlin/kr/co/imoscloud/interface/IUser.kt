package kr.co.imoscloud.`interface`

import jakarta.servlet.http.HttpServletRequest
import kr.co.imoscloud.entity.User
import kr.co.imoscloud.fetcher.UserFetcher

interface IUser {
    companion object {
        val IMOS = "imos"
        val PEMS = "pems"
    }

    fun getSiteByDomain(req: HttpServletRequest): String {
        val domain = req.serverName
        return when (domain) {
            "http://imos-cloud.co.kr", "http://localhost:3000" -> IMOS
            "http://pems-cloud.co.kr" -> PEMS
            else -> throw IllegalArgumentException("지원하는 도멘인이 아닙니다. ")
        }
    }

    fun userToUserOutput(user: User?): UserFetcher.UserOutput {
        return user
            ?.let { UserFetcher.UserOutput(
                userId = user.userId,
                userNm = user.userName,
                email = user.userEmail,
                roleId = user.roleId,
                message = "${user.userId} 로그인 성공"
            )}
            ?:run { UserFetcher.UserOutput(message = "로그인 실패") }
    }
}