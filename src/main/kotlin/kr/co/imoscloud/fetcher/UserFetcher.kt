package kr.co.imoscloud.fetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.dto.ExistLoginIdRequest
import kr.co.imoscloud.dto.RoleResponseForSelect
import kr.co.imoscloud.dto.UserDetail
import kr.co.imoscloud.dto.UserInput
import kr.co.imoscloud.service.UserRoleService
import kr.co.imoscloud.service.UserService

@DgsComponent
class UserFetcher(
    private val userService: UserService,
    private val userRoleService: UserRoleService
) {

    @DgsMutation
    fun signUp(@InputArgument("input") input: UserInput) { userService.signUp(input) }

    @DgsQuery
    fun existLoginId(@InputArgument("input") input: ExistLoginIdRequest): Boolean {
        return userService.existLoginId(input)
    }

    @DgsQuery
    fun getUserGroup(): List<UserDetail?> = userService.getUserGroupByCompany()

    @DgsQuery
    fun getUserDetail(@InputArgument("id") id: Long): UserDetail = userService.getUserDetail(id)

    @DgsQuery
    fun getRoles(): List<RoleResponseForSelect> = userRoleService.getUserRoleGroup()
}
