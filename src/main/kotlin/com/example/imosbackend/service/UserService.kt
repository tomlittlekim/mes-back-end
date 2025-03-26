package com.example.imosbackend.service

import com.example.imosbackend.dto.UserRequest
import com.example.imosbackend.entity.User
import com.example.imosbackend.repository.UserRepository
import com.example.imosbackend.security.UserPrincipal
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class UserService(
    private val userRepo: UserRepository
) {

    fun signUp(req: UserRequest, loginUser: UserPrincipal): User {
        if (!checkRole(loginUser)) throw IllegalArgumentException("관리자 이상의 등급을 가진 유저가 아닙니다. ")

        val modifyReq = modifyReqByRole(loginUser, req)
        val newUser = try {
            val target = userRepo.findBySiteAndUserIdForSignUp(modifyReq.site!!, modifyReq.userId)!!
            if (target.isActive == false) target.apply { isActive = true; createCommonCol(loginUser) }
            else throw IllegalArgumentException("이미 존재하는 유저입니다. ")
        } catch (e: NullPointerException) {
            generateUser(req)
        }

        return userRepo.save(newUser)
    }

    private fun checkRole(loginUser: UserPrincipal): Boolean {
        return loginUser.authorities.first().authority == "admin"
    }

    private fun modifyReqByRole(loginUser: UserPrincipal, req: UserRequest): UserRequest {
        val isDev = loginUser.authorities.first().authority == "dev"
        if (isDev && req.site == null || isDev && req.compCd == null)
            throw IllegalArgumentException("site 또는 compCd 가 비어있습니다. ")

        return req.apply {
            this.site = if (isDev) req.site else loginUser.getSite()
            this.compCd = if (isDev) req.compCd else loginUser.getCompCd()
        }
    }

    private fun generateUser(req: UserRequest): User {
        val uuid =  UUID.randomUUID().toString()
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        return User(
            site = req.site!!,
            compCd = req.compCd!!,
            userId = req.userId ?: (uuid.substring(0, 7) + formatter.format(today)),
            userPwd = uuid.substring(7, 14) +"!@",
            roleId = req.roleId
        )
    }
}