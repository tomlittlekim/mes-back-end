package kr.co.imoscloud.service.standardInfo

import jakarta.transaction.Transactional
import kr.co.imoscloud.entity.standardInfo.Code
import kr.co.imoscloud.entity.standardInfo.CodeClass
import kr.co.imoscloud.entity.standardInfo.Factory
import kr.co.imoscloud.fetcher.standardInfo.*
import kr.co.imoscloud.repository.CodeClassRep
import kr.co.imoscloud.repository.CodeRep
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class CommonCodeService(
    val codeClassRep: CodeClassRep,
    val codeRep:CodeRep
) {
    fun getCodeClass(filter: CodeClassFilter): List<CodeClassResponse> {
        val codeClassList = codeClassRep.getCodeClassList(
            site = "imos",
            compCd = "eightPin",
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
        //TODO 저장 ,수정시 공통 으로 작성자 ,작성일 ,수정자 ,수정일 변경 저장이 필요함
        createdRows.filterNotNull().takeIf { it.isNotEmpty() }?.let{ createCodeClass(it) }
        updatedRows.filterNotNull().takeIf { it.isNotEmpty() }?.let{ updateCodeClass(it) }
    }

    fun createCodeClass(createdRows: List<CodeClassInput?>){
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val codeClassList = createdRows.map {
            CodeClass(
                codeClassId = "CD" + LocalDateTime.now().format(formatter) +
                        System.nanoTime().toString().takeLast(3),  // 3자리만 사용,
                site = "imos",
                compCd = "eightPin",
                codeClassName = it?.codeClassName,
                codeClassDesc = it?.codeClassDesc,
                createUser = "syh"
            )
        }

        codeClassRep.saveAll(codeClassList)
    }

    fun updateCodeClass(updatedRows: List<CodeClassUpdate?>){
        val codeClassIds = updatedRows.map {
            it?.codeClassId
        }

        val codeClassList = codeClassRep.getCodeClassListByIds(
            site = "imos",
            compCd = "eightPin",
            codeClassIds = codeClassIds
        )

        val updateList = codeClassList.associateBy { it?.codeClassId }

        updatedRows.forEach{ x ->
            val codeClassId = x?.codeClassId
            val codeClass = updateList[codeClassId]

            codeClass?.let{
                it.codeClassName = x?.codeClassName
                it.codeClassDesc = x?.codeClassDesc
            }
        }

        codeClassRep.saveAll(codeClassList)
    }

    fun getCodes(codeClassId: String):List<CodeResponse>{
        val codeList = codeRep.getCodeList(
            site = "imos",
            compCd = "eightPin",
            codeClassId = codeClassId
        )

        val result = entityToResponse(codeList)

        return result
    }

    @Transactional
    fun saveCode(createdRows: List<CodeInput?>, updatedRows:List<CodeUpdate?>){
        //TODO 저장 ,수정시 공통 으로 작성자 ,작성일 ,수정자 ,수정일 변경 저장이 필요함
        createdRows.filterNotNull().takeIf { it.isNotEmpty() }?.let { createCode(it) }
        updatedRows.filterNotNull().takeIf { it.isNotEmpty() }?.let { updateCode(it) }
    }

    fun createCode(createdRows: List<CodeInput?>){
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

        val codeList = createdRows.map {
            Code(
                codeClassId = it?.codeClassId,
                codeId = "C" + LocalDateTime.now().format(formatter) +
                        System.nanoTime().toString().takeLast(3),  // 3자리만 사용,
                site = "imos",
                compCd = "eightPin",
                codeName = it?.codeName,
                codeDesc = it?.codeDesc,
                sortOrder = it?.sortOrder,
                flagActive = it?.flagActive.equals("Y"),
                createUser = "syh"
            )
        }

        codeRep.saveAll(codeList)
    }

    fun updateCode(updatedRows: List<CodeUpdate>){
        val codeIds = updatedRows.map {
            it.codeId
        }

        val codeList = codeRep.getCodeListByIds(
            site = "imos",
            compCd = "eightPin",
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
                it.flagActive = x.flagActive.equals("Y")
            }
        }

        codeRep.saveAll(codeList)
    }

    fun deleteCode(codeId:String): Boolean {
        return codeRep.deleteByCodeId(
            site = "imos",
            compCd = "eightPin",
            codeId
        ) > 0
    }

    fun getGridCodes(codeClassId: String):List<CodeResponse>{
        val codeList = codeRep.getGridCodes(
            site = "imos",
            compCd = "eightPin",
            codeClassId = codeClassId
        )

        val result = entityToResponse(codeList)

        return result
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
            )
        }
    }

}

data class CodeClassResponse(
    val codeClassId:String ?= null,
    val codeClassName:String ?= null,
    val codeClassDesc:String ?= null
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