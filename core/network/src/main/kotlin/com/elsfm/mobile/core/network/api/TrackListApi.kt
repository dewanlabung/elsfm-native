package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class PlaylistTracksPagination(val data: List<Track>)

@Serializable
private data class PlaylistTracksResponse(val pagination: PlaylistTracksPagination)

class TrackListApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun getPlaylistTracks(playlistId: Int): ApiResult<List<Track>> {
        return try {
            val response = httpClient.get("api/v1/playlists/$playlistId/tracks")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<PlaylistTracksResponse>().pagination.data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
