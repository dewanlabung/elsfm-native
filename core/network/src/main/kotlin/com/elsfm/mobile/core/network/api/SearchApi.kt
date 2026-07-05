package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.SearchResult
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class SearchResultList(val data: List<SearchResult>)

class SearchApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun search(query: String): ApiResult<List<SearchResult>> {
        return try {
            val response = httpClient.get("api/v1/search?q=$query")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<SearchResultList>().data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
