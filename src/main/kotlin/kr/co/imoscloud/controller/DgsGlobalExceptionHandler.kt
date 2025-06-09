package kr.co.imoscloud.controller

import com.netflix.graphql.dgs.exceptions.DgsBadRequestException
import graphql.GraphQLError
import graphql.schema.DataFetchingEnvironment
import kr.co.imoscloud.exception.ImosException
import kr.co.imoscloud.model.exception.CustomGraphQLError
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.stereotype.Component

@Component
class DgsGlobalExceptionHandler : DataFetcherExceptionResolverAdapter() {
    override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? {
        return when (ex) {
            is ImosException -> CustomGraphQLError(
                errorMessage = ex.errorCode.message,
                errorExtensions = mapOf(
                    "code" to ex.errorCode.code,
                    "status" to ex.errorCode.status
                )
            )
            is DgsBadRequestException -> CustomGraphQLError(
                errorMessage = ex.message ?: "잘못된 요청입니다.",
                errorExtensions = mapOf("code" to "BAD_REQUEST", "status" to 400)
            )
            else -> null // 기본 DGS 핸들러
        }
    }
}