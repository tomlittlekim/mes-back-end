package kr.co.imoscloud.fetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.dto.*
import kr.co.imoscloud.entity.user.UserRole
import kr.co.imoscloud.service.UserRoleService
import kr.co.imoscloud.service.UserService

@DgsComponent
class UserFetcher(
    private val userService: UserService,
    private val userRoleService: UserRoleService
) {

    @DgsMutation
    fun upsertUser(@InputArgument("req") req: UserInput) { userService.upsertUser(req) }

    @DgsQuery
    fun existLoginId(@InputArgument("req") req: ExistLoginIdRequest): Boolean {
        return userService.existLoginId(req)
    }

    @DgsQuery
    fun getUserGroup(@InputArgument("req") req: UserGroupRequest?): List<UserSummery?> =
        userService.getUserGroupByCompany(req)

    @DgsQuery
    fun getUserDetail(@InputArgument("id") id: Long): UserDetail = userService.getUserDetail(id)

    @DgsQuery
    fun getRoles(): List<UserRole> = userRoleService.getUserRoleGroup()

    @DgsMutation
    fun deleteUser(@InputArgument("id") id: Long) = userService.deleteUser(id)
}
