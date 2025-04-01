package kr.co.imoscloud.fetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.dto.UserInput
import kr.co.imoscloud.security.UserPrincipal
import kr.co.imoscloud.service.UserService
import org.springframework.security.core.annotation.AuthenticationPrincipal

@DgsComponent
class UserFetcher(
    private val userService: UserService
) {

    @DgsMutation
    fun signUp(@InputArgument("input") input: UserInput, @AuthenticationPrincipal user: UserPrincipal) {
        userService.signUp(input, user)
    }
}
