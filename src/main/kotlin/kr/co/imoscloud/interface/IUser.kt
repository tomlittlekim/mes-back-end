package kr.co.imoscloud.`interface`

import jakarta.servlet.http.HttpServletRequest

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
}