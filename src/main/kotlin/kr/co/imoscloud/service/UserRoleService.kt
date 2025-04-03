package kr.co.imoscloud.service

import kr.co.imoscloud.core.Core
import kr.co.imoscloud.dto.RoleResponseForSelect
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service

@Service
class UserRoleService(
    val core: Core
) {

    fun getUserRoleGroup(): List<RoleResponseForSelect> {
        val loginUser = SecurityUtils.getCurrentUserPrincipal()
        return core.roleRepo.getRolesByCompany(loginUser.getSite(), loginUser.compCd)
            .map { RoleResponseForSelect(it.roleId, it.roleName) }
    }

}