package kr.co.imoscloud.service.standardInfo

import kr.co.imoscloud.fetcher.standardInfo.CodeClassFilter
import kr.co.imoscloud.repository.CodeClassRep
import org.springframework.stereotype.Service

@Service
class CommonCodeService(
    val codeClassRep: CodeClassRep
) {

    fun getCodeClass(filter: CodeClassFilter): List<codeClassResponse> {
        val codeClassList = codeClassRep.getCodeClassList(
            site = "imos",
            compCd = "eightPin",
            codeClassId = filter.codeClassId,
            codeClassName = filter.codeClassName
        )

        return codeClassList.map {
            codeClassResponse(
                it?.codeClassId,
                it?.codeClassName,
                it?.codeClassDesc
            )
        }
    }

}

data class codeClassResponse(
    val codeClassId:String ?= null,
    val codeClassName:String ?= null,
    val codeClassDesc:String ?= null
)