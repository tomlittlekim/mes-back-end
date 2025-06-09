package kr.co.imoscloud.model.exception

import graphql.GraphQLError
import graphql.language.SourceLocation

data class CustomGraphQLError(
    private val errorMessage: String,
    private val errorExtensions: Map<String, Any?>? = null
) : GraphQLError {
    override fun getMessage(): String = errorMessage
    override fun getExtensions(): Map<String, Any?>? = errorExtensions
    override fun getLocations(): List<SourceLocation>? = null
    override fun getPath(): List<Any>? = null
    override fun getErrorType() = null
}