package com.elsfm.mobile.core.network

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class ValidationError(val fields: Map<String, List<String>>) : ApiResult<Nothing>
    data object Unauthorized : ApiResult<Nothing>
    data class NetworkError(val cause: Throwable) : ApiResult<Nothing>
}
