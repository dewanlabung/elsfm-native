package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

private const val REPOSTABLE_TYPE_TRACK = "track"
private const val REPOSTABLE_TYPE_ALBUM = "album"

@Serializable
data class RepostToggleResponse(
    val action: String,
)

@Serializable
private data class RepostToggleRequest(
    @SerialName("repostable_type") val repostableType: String,
    @SerialName("repostable_id") val repostableId: Int,
)

/**
 * Backed by the real Laravel `RepostController::toggle` route:
 * `POST api/v1/reposts/toggle` with `{repostable_type, repostable_id}`. The endpoint toggles
 * (creates or deletes) the current user's repost and returns `{action: "added" | "removed"}`.
 */
class RepostApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun toggleTrackRepost(trackId: Int): ApiResult<RepostToggleResponse> =
        toggleRepost(REPOSTABLE_TYPE_TRACK, trackId)

    suspend fun toggleAlbumRepost(albumId: Int): ApiResult<RepostToggleResponse> =
        toggleRepost(REPOSTABLE_TYPE_ALBUM, albumId)

    private suspend fun toggleRepost(repostableType: String, repostableId: Int): ApiResult<RepostToggleResponse> {
        return try {
            val response = httpClient.post("api/v1/reposts/toggle") {
                contentType(ContentType.Application.Json)
                setBody(
                    RepostToggleRequest(
                        repostableType = repostableType,
                        repostableId = repostableId,
                    ),
                )
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<RepostToggleResponse>())
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
