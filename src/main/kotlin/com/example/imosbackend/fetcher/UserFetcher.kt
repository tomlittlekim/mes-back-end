package com.example.imosbackend.fetcher

import com.example.imosbackend.security.UserPrincipal
import com.example.imosbackend.service.UserService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.InputArgument
import org.springframework.security.core.annotation.AuthenticationPrincipal

@DgsComponent
class UserFetcher(
    private val userService: UserService
) {

    @DgsMutation
    fun signUp(@InputArgument("input") input: UserInput, @AuthenticationPrincipal user: UserPrincipal) {
        userService.signUp(input, user)
    }

    data class UserInput(
        val id: Long? = null,
        var site: String?=null,
        var compCd: String?=null,
        var userId: String?=null,
        var password: String?=null,
        var userNm: String?=null,
        var email: String?=null,
        var roleId: String?=null,
        var phoneNum: String?=null,
        var departmentId: String?=null,
        var textarea: String?=null,
    )
} 