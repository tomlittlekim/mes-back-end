package kr.co.imoscloud.fetcher.standardInfo

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import kr.co.imoscloud.service.standardInfo.CodeClassResponse
import kr.co.imoscloud.service.standardInfo.CodeResponse
import kr.co.imoscloud.service.standardInfo.CommonCodeService

@DgsComponent
class CommonCodeFetcher(
    val commonCodeService: CommonCodeService,
) {

    @DgsQuery
    fun getCodeClass(@InputArgument("filter") filter: CodeClassFilter) : List<CodeClassResponse> {
        return commonCodeService.getCodeClass(filter)
    }

    @DgsMutation
    fun saveCodeClass(
        @InputArgument("createdRows") createdRows: List<CodeClassInput?>,
        @InputArgument("updatedRows") updatedRows:List<CodeClassUpdate?>
    ): Boolean {
        commonCodeService.saveCodeClass(createdRows, updatedRows)
        return true
    }

    @DgsQuery
    fun getCodes(@InputArgument("codeClassId") codeClassId: String): List<CodeResponse> {
        return commonCodeService.getCodes(codeClassId)
    }

    @DgsMutation
    fun saveCode(
        @InputArgument("createdRows") createdRows: List<CodeInput?>,
        @InputArgument("updatedRows") updatedRows: List<CodeUpdate?>
    ): Boolean {
        commonCodeService.saveCode(createdRows, updatedRows)
        return true
    }

    @DgsMutation
    fun deleteCode(@InputArgument("codeId") codeId: String): Boolean {
        return commonCodeService.deleteCode(codeId)
    }

    @DgsQuery
    fun getGridCodes(@InputArgument("codeClassId") codeClassId: String): List<CodeResponse> {
        return commonCodeService.getGridCodes(codeClassId)
    }

    @DgsQuery
    fun getInitialCodes(@InputArgument("codeClassId") codeClassId: String): List<CodeResponse> {
        return commonCodeService.getInitialCodes(codeClassId)
    }
}

data class CodeClassFilter(
    val codeClassId: String,
    val codeClassName: String,
)

data class CodeClassInput(
    val codeClassName: String,
    val codeClassDesc: String
)

data class CodeInput(
    val codeClassId: String,
    val codeName: String,
    val codeDesc: String,
    val sortOrder: Int,
)

data class CodeUpdate(
    val codeClassId: String,
    val codeId:String,
    val codeName: String,
    val codeDesc: String,
    val sortOrder: Int,
)

data class CodeClassUpdate(
    val codeClassId: String,
    val codeClassName: String,
    val codeClassDesc: String
)