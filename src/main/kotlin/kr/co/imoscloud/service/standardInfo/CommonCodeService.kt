package kr.co.imoscloud.service.standardInfo

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.standardInfo.Code
import kr.co.imoscloud.entity.standardInfo.CodeClass
import kr.co.imoscloud.fetcher.standardInfo.*
import kr.co.imoscloud.repository.CodeClassRep
import kr.co.imoscloud.repository.CodeRep
import kr.co.imoscloud.security.UserPrincipal
import kr.co.imoscloud.util.SecurityUtils
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class CommonCodeService(
    val codeClassRep: CodeClassRep,
    val codeRep:CodeRep
) {
    fun getCodeClass(filter: CodeClassFilter): List<CodeClassResponse> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        val codeClassList = codeClassRep.getCodeClassList(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            codeClassId = filter.codeClassId,
            codeClassName = filter.codeClassName
        )

        return codeClassList.map {
            CodeClassResponse(
                it?.codeClassId,
                it?.codeClassName,
                it?.codeClassDesc
            )
        }
    }

    @Transactional
    fun saveCodeClass(createdRows: List<CodeClassInput?>, updatedRows:List<CodeClassUpdate?>){
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        createdRows.filterNotNull().takeIf { it.isNotEmpty() }?.let{ createCodeClass(it,userPrincipal) }
        updatedRows.filterNotNull().takeIf { it.isNotEmpty() }?.let{ updateCodeClass(it,userPrincipal) }
    }

    fun createCodeClass(createdRows: List<CodeClassInput?>, userPrincipal: UserPrincipal){
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val codeClassList = createdRows.map {
            CodeClass(
                codeClassId = "CD" + LocalDateTime.now().format(formatter) +
                        System.nanoTime().toString().takeLast(3),  // 3자리만 사용,
                site = userPrincipal.getSite(),
                compCd = userPrincipal.compCd,
                codeClassName = it?.codeClassName,
                codeClassDesc = it?.codeClassDesc,
            ).apply{ createCommonCol(userPrincipal) }
        }

        codeClassRep.saveAll(codeClassList)
    }

    fun updateCodeClass(updatedRows: List<CodeClassUpdate?>, userPrincipal: UserPrincipal){
        val codeClassIds = updatedRows.map {
            it?.codeClassId
        }

        val codeClassList = codeClassRep.getCodeClassListByIds(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            codeClassIds = codeClassIds
        )

        val updateList = codeClassList.associateBy { it?.codeClassId }

        updatedRows.forEach{ x ->
            val codeClassId = x?.codeClassId
            val codeClass = updateList[codeClassId]

            codeClass?.let{
                it.codeClassName = x?.codeClassName
                it.codeClassDesc = x?.codeClassDesc
                it.updateCommonCol(userPrincipal)
            }
        }

        codeClassRep.saveAll(codeClassList)
    }

    fun getCodes(codeClassId: String):List<CodeResponse>{
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        val codeList = codeRep.getCodeList(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            codeClassId = codeClassId
        )

        val result = entityToResponse(codeList)

        return result
    }

    @Transactional
    fun saveCode(createdRows: List<CodeInput?>, updatedRows:List<CodeUpdate?>){
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()
        createdRows.filterNotNull().takeIf { it.isNotEmpty() }?.let { createCode(it, userPrincipal) }
        updatedRows.filterNotNull().takeIf { it.isNotEmpty() }?.let { updateCode(it, userPrincipal) }
    }

    fun createCode(createdRows: List<CodeInput?>, userPrincipal: UserPrincipal){
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val codeList = createdRows.map {
            Code(
                codeClassId = it?.codeClassId,
                codeId = "C" + LocalDateTime.now().format(formatter) +
                        System.nanoTime().toString().takeLast(3),  // 3자리만 사용,
                site = userPrincipal.getSite(),
                compCd = userPrincipal.compCd,
                codeName = it?.codeName,
                codeDesc = it?.codeDesc,
                sortOrder = it?.sortOrder,
            ).apply {
                createCommonCol(userPrincipal)
            }
        }

        codeRep.saveAll(codeList)
    }

    fun updateCode(updatedRows: List<CodeUpdate>, userPrincipal: UserPrincipal){
        val codeIds = updatedRows.map {
            it.codeId
        }

        val codeList = codeRep.getCodeListByIds(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            codeClassId = updatedRows[0].codeClassId,
            codeIds = codeIds
        )

        val updateList = codeList.associateBy { it?.codeId }

        updatedRows.forEach{ x ->
            val codeId = x.codeId
            val code = updateList[codeId]

            code?.let{
                it.codeName = x.codeName
                it.codeDesc = x.codeDesc
                it.sortOrder = x.sortOrder
                it.updateCommonCol(userPrincipal)
            }
        }

        codeRep.saveAll(codeList)
    }

    fun deleteCode(codeIds:List<String>): Boolean {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        return codeRep.deleteByCodeId(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            codeIds = codeIds,
            updateUser = userPrincipal.loginId
        ) > 0
    }

    fun getGridCodes(codeClassId: String):List<CodeResponse>{
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        val codeList = codeRep.getGridCodes(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            codeClassId = codeClassId
        )

        val result = entityToResponse(codeList)

        return result
    }

    fun getGridCodeList(codeClassIds: List<String>): List<CodeClassListResponse> {
        val userPrincipal = SecurityUtils.getCurrentUserPrincipal()

        val codeClassList = codeClassRep.getCodeClassListByIds(
            site = userPrincipal.getSite(),
            compCd = userPrincipal.compCd,
            codeClassIds = codeClassIds
        )

        return codeClassList.map { codeClass ->
            val codes = codeRep.getGridCodes(
                site = userPrincipal.getSite(),
                compCd = userPrincipal.compCd,
                codeClassId = codeClass?.codeClassId ?: ""
            )

            CodeClassListResponse(
                codeClassId = codeClass?.codeClassId,
                codeClassName = codeClass?.codeClassName,
                codeClassDesc = codeClass?.codeClassDesc,
                codes = entityToResponse(codes)
            )
        }
    }


    fun getInitialCodes(codeClassId: String):List<CodeResponse> {
        val results = codeRep.getInitialCodes(codeClassId)
        return entityToResponse(results)
    }

    private fun entityToResponse(codeList:List<Code?>): List<CodeResponse> {
        return codeList.map{
            CodeResponse(
                codeClassId = it?.codeClassId,
                codeId = it?.codeId,
                codeName = it?.codeName,
                codeDesc = it?.codeDesc,
                sortOrder = it?.sortOrder,
                flagActive = if (it?.flagActive == true) "Y" else "N",
                createUser = it?.createUser,
                createDate = it?.createDate.toString().replace("T", " "),
                updateUser = it?.updateUser,
                updateDate = it?.updateDate.toString().replace("T", " "),
            )
        }
    }

}

data class CodeClassResponse(
    val codeClassId:String ?= null,
    val codeClassName:String ?= null,
    val codeClassDesc:String ?= null
)

data class CodeClassListResponse(
    val codeClassId: String? = null,
    val codeClassName: String? = null,
    val codeClassDesc: String? = null,
    val codes: List<CodeResponse> = emptyList(),
)

data class CodeResponse(
    val codeClassId:String ?= null,
    val codeId:String ?= null,
    val codeName:String ?= null,
    val codeDesc:String ?= null,
    val sortOrder: Int ?= null,
    val flagActive:String ?= null,
    val createUser:String ?= null,
    val createDate:String ?= null,
    val updateUser:String ?= null,
    val updateDate:String ?= null,
)