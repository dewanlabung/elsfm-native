package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Recommendation
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class RecommendationList(val data: List<Recommendation>)

class RecommendationApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun getRecommendations(basedOn: Int): ApiResult<List<Recommendation>> {
        return try {
            val response = httpClient.get("api/v1/recommendations") {
                parameter("based_on", basedOn)
                parameter("limit", 10)
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<RecommendationList>().data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
