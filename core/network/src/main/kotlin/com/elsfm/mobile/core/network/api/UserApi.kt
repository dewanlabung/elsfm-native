package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.FollowState
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class FollowResponse(
    val following: Boolean,
    val timestamp: String,
)

class UserApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun isArtistFollowed(artistId: Int): ApiResult<FollowState> {
        return try {
            val response = httpClient.get("api/v1/me/follows/artists/$artistId")
            if (response.status.isSuccess()) {
                val followResponse = response.body<FollowResponse>()
                ApiResult.Success(
                    FollowState(
                        following = followResponse.following,
                        timestamp = followResponse.timestamp,
                    )
                )
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend fun followArtist(artistId: Int): ApiResult<FollowState> {
        return try {
            val response = httpClient.post("api/v1/me/follows/artists/$artistId")
            if (response.status.isSuccess()) {
                val followResponse = response.body<FollowResponse>()
                ApiResult.Success(
                    FollowState(
                        following = followResponse.following,
                        timestamp = followResponse.timestamp,
                    )
                )
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend fun unfollowArtist(artistId: Int): ApiResult<FollowState> {
        return try {
            val response = httpClient.delete("api/v1/me/follows/artists/$artistId")
            if (response.status.isSuccess()) {
                val followResponse = response.body<FollowResponse>()
                ApiResult.Success(
                    FollowState(
                        following = followResponse.following,
                        timestamp = followResponse.timestamp,
                    )
                )
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
