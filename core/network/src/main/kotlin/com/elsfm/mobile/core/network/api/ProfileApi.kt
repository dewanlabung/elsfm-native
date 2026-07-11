package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.model.UserProfile
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class UpdateProfileRequest(val name: String, val bio: String?)

// `api/v1/me/recently-played` does not exist on the real backend (verified
// against routes/api.php). The real play-history endpoint is
// `GET api/v1/tracks/plays/{userId}` (TrackPlaysController::index), which
// accepts the literal string "me" for the current user and returns a
// standard paginated response, same shape as playlist/album track lists.
@Serializable
private data class RecentlyPlayedPagination(val data: List<Track>)

@Serializable
private data class RecentlyPlayedResponse(val pagination: RecentlyPlayedPagination)

open class ProfileApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    open suspend fun getProfile(): ApiResult<UserProfile> {
        return try {
            val response = httpClient.get("api/v1/me/profile")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<UserProfile>())
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    open suspend fun updateProfile(name: String, bio: String?): ApiResult<UserProfile> {
        return try {
            val response = httpClient.put("api/v1/me/profile") {
                contentType(ContentType.Application.Json)
                setBody(UpdateProfileRequest(name = name, bio = bio))
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<UserProfile>())
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    open suspend fun getRecentlyPlayed(): ApiResult<List<Track>> {
        return try {
            val response = httpClient.get("api/v1/tracks/plays/me")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<RecentlyPlayedResponse>().pagination.data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
