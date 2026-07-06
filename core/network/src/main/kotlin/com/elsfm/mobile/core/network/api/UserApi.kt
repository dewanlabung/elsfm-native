package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.FollowState
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class FollowResponse(
    val following: Boolean,
    val timestamp: String,
)

@Serializable
data class ShareTrackResponse(
    @kotlinx.serialization.SerialName("share_url")
    val shareUrl: String,
)

@Serializable
private data class LikeableRequestItem(
    @SerialName("likeable_id") val likeableId: Int,
    @SerialName("likeable_type") val likeableType: String,
)

@Serializable
private data class LikeablesRequest(val likeables: List<LikeableRequestItem>)

private const val LIKEABLE_TYPE_TRACK = "track"

class UserApi @Inject constructor(
    private val httpClient: HttpClient,
) : UserApiLike {
    override suspend fun isArtistFollowed(artistId: Int): ApiResult<FollowState> {
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

    override suspend fun followArtist(artistId: Int): ApiResult<FollowState> {
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

    override suspend fun unfollowArtist(artistId: Int): ApiResult<FollowState> {
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

    suspend fun shareTrack(trackId: Int): ApiResult<ShareTrackResponse> {
        return try {
            val response = httpClient.post("api/v1/tracks/$trackId/share")
            if (response.status.isSuccess()) {
                val shareResponse = response.body<ShareTrackResponse>()
                ApiResult.Success(shareResponse)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    /**
     * Adds the given track to the current user's library ("like" a track).
     *
     * Backed by the real Laravel endpoint (`UserLibraryTracksController::addToLibrary`):
     * `POST api/v1/users/me/add-to-library` with a `likeables` array body. There is no
     * per-track "is liked" check endpoint or inline `is_liked` field on [Track][com.elsfm.mobile.core.model.Track] —
     * the backend only exposes `GET users/{user}/liked-tracks` (a paginated list), matching
     * how the web client derives liked-state client-side rather than from a boolean field.
     * On success we return `true` since the caller already knows the intended new state.
     */
    suspend fun addTrackToLibrary(trackId: Int): ApiResult<Boolean> =
        postLikeable("api/v1/users/me/add-to-library", trackId, likedResult = true)

    /**
     * Removes the given track from the current user's library ("unlike" a track).
     *
     * Backed by `POST api/v1/users/me/remove-from-library` (also POST, not DELETE,
     * per the backend route definition) with the same `likeables` array body shape.
     */
    suspend fun removeTrackFromLibrary(trackId: Int): ApiResult<Boolean> =
        postLikeable("api/v1/users/me/remove-from-library", trackId, likedResult = false)

    private suspend fun postLikeable(
        path: String,
        trackId: Int,
        likedResult: Boolean,
    ): ApiResult<Boolean> {
        return try {
            val response = httpClient.post(path) {
                contentType(ContentType.Application.Json)
                setBody(
                    LikeablesRequest(
                        likeables = listOf(
                            LikeableRequestItem(likeableId = trackId, likeableType = LIKEABLE_TYPE_TRACK),
                        ),
                    ),
                )
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(likedResult)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
