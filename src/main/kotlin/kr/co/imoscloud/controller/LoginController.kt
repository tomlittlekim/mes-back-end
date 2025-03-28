package kr.co.imoscloud.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.co.imoscloud.fetcher.UserFetcher.UserOutput
import kr.co.imoscloud.service.UserService
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class LoginController(
    private val userService: UserService
) {

    @GetMapping("/api/login")
    fun login(loginReq: LoginRequest, req: HttpServletRequest, res: HttpServletResponse): UserOutput {
        return userService.signIn(loginReq, req, res)
    }

    data class LoginRequest(val userId: String, val userPwd: String)
}