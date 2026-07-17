package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class PlaylistTracksPagination(
    val data: List<Track>,
    @SerialName("next_page") val nextPage: Int? = null,
)

@Serializable
private data class PlaylistTracksResponse(val pagination: PlaylistTracksPagination)

/** One page of playlist tracks. [hasMore] mirrors the backend's `next_page` (null once exhausted). */
data class PlaylistTracksPage(val tracks: List<Track>, val hasMore: Boolean)

class TrackListApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun getPlaylistTracks(playlistId: Int, page: Int = 1): ApiResult<PlaylistTracksPage> {
        return try {
            val response = httpClient.get("api/v1/playlists/$playlistId/tracks") {
                parameter("page", page)
            }
            if (response.status.isSuccess()) {
                val pagination = response.body<PlaylistTracksResponse>().pagination
                ApiResult.Success(PlaylistTracksPage(pagination.data, pagination.nextPage != null))
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
