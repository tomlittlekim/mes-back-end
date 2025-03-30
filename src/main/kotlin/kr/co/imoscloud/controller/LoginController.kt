package kr.co.imoscloud.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.co.imoscloud.fetcher.UserFetcher.UserOutput
import kr.co.imoscloud.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Controller
class LoginController(
    private val userService: UserService
) {

    @PostMapping("/api/login")
    fun login(
        @RequestBody loginReq: LoginRequest,
        req: HttpServletRequest,
        res: HttpServletResponse
    ): ResponseEntity<UserOutput> {
        return userService.signIn(loginReq, req, res)
    }

    data class LoginRequest(val userId: String, val userPwd: String)
}