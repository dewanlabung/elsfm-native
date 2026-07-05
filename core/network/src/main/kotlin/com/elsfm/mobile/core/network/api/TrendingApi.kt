package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.TrendingTrack
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class TrendingTrackList(val data: List<TrendingTrack>)

class TrendingApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun getTrending(): ApiResult<List<TrendingTrack>> {
        return try {
            val response = httpClient.get("api/v1/trending") {
                parameter("type", "tracks")
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<TrendingTrackList>().data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
