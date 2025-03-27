package kr.co.imoscloud.fetcher.standardInfo

import kr.co.imoscloud.service.standardInfo.CommonCodeService
import kr.co.imoscloud.service.standardInfo.codeClassResponse
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument

@DgsComponent
class CommonCodeFetcher(
    val commonCodeService: CommonCodeService,
) {

    @DgsQuery
    fun getCodeClass(@InputArgument("filter") filter: CodeClassFilter) : List<codeClassResponse> {
        return commonCodeService.getCodeClass(filter)
    }

    @DgsMutation
    fun saveCodeClass(
        @InputArgument("createdRows") createdRows: List<CodeClassInput?>,
        @InputArgument("updatedRows") updatedRows:List<CodeClassUpdate?>
    ): Boolean {

        return true
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

data class CodeClassUpdate(
    val codeClassId: String,
    val codeClassName: String,
    val codeClassDesc: String
)