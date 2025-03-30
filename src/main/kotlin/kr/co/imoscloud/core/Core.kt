package kr.co.imoscloud.core

import kr.co.imoscloud.dto.RoleSummery
import kr.co.imoscloud.entity.user.User
import kr.co.imoscloud.entity.user.UserRole
import kr.co.imoscloud.repository.user.UserRepository
import kr.co.imoscloud.repository.user.UserRoleRepository
import org.springframework.stereotype.Component

@Component
class Core(
    userRepo: UserRepository,
    roleRepo: UserRoleRepository
): AbstractInitialSetting(userRepo, roleRepo) {
    override fun getAllUsersDuringInspection(indies: List<Long>): MutableMap<Long, String?> {
        val userList: List<User> = if (indies.size == 1) {
            userRepo.findById(indies.first()).map(::listOf)!!.orElseGet { emptyList<User>() }
        } else userRepo.findAllByIdIn(indies)

        return userList.associate { it.id to it.userName }.toMutableMap()
    }

    override fun getAllRolesDuringInspection(indies: List<Long>): MutableMap<Long, RoleSummery?> {
        val roleList: List<UserRole> = if (indies.size == 1) {
            roleRepo.findById(indies.first()).map(::listOf).orElseGet { emptyList<UserRole>() }
        } else roleRepo.findAllByIdIn(indies)

        return roleList.associate {
            val summery = RoleSummery(it.roleName, it.priorityLevel)
            it.id to summery
        }.toMutableMap()
    }
}