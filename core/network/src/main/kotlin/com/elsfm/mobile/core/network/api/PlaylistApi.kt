package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

/**
 * Matches `PlaylistLoader::toApiResource` - `description` and `updated_at` are only
 * present with the `playlistPage`/`editPlaylistPage` loader (present for [getPlaylist],
 * absent for [getUserPlaylists]'s plain list items); `tracks_count` is only present
 * when the caller ran `loadCount('tracks')` first, which neither endpoint used here does.
 * All three are therefore optional.
 */
@Serializable
data class PlaylistInfo(
    val id: Int,
    val name: String,
    val description: String? = null,
    val image: String? = null,
    @SerialName("tracks_count")
    val trackCount: Int? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
private data class PlaylistInfoDetail(val playlist: PlaylistInfo)

@Serializable
private data class PlaylistInfoPagination(val data: List<PlaylistInfo>)

@Serializable
private data class UserPlaylistsResponse(val pagination: PlaylistInfoPagination)

@Serializable
data class PaginatedTracks(
    val data: List<Track>,
    val total: Int,
    @SerialName("per_page")
    val perPage: Int,
    @SerialName("current_page")
    val currentPage: Int,
)

@Serializable
private data class PaginatedTracksResponse(val pagination: PaginatedTracks)

class PlaylistApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun getPlaylist(id: Int): ApiResult<PlaylistInfo> {
        return try {
            val response = httpClient.get("api/v1/playlists/$id")
            if (response.status.isSuccess()) {
                val playlist = response.body<PlaylistInfoDetail>().playlist
                ApiResult.Success(playlist)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend fun getPlaylistTracks(
        id: Int,
        limit: Int = 50,
        offset: Int = 0,
    ): ApiResult<PaginatedTracks> {
        return try {
            val response = httpClient.get("api/v1/playlists/$id/tracks") {
                parameter("limit", limit)
                parameter("offset", offset)
            }
            if (response.status.isSuccess()) {
                val tracks = response.body<PaginatedTracksResponse>().pagination
                ApiResult.Success(tracks)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    /**
     * Real endpoint (`UserPlaylistsController::index`): `GET api/v1/users/{id}/playlists`.
     * When `id` is the signed-in user, this returns all of their playlists (public and
     * private); for any other user it's filtered to public ones. Used for the
     * "Add to playlist" picker.
     */
    suspend fun getUserPlaylists(userId: Int): ApiResult<List<PlaylistInfo>> {
        return try {
            val response = httpClient.get("api/v1/users/$userId/playlists")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<UserPlaylistsResponse>().pagination.data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    /**
     * Real endpoint is `POST api/v1/playlists/{id}/tracks/add`
     * (`PlaylistTracksController::add`), which reads track ids from an `ids` array,
     * not a singular `trackId` field.
     */
    suspend fun addTrackToPlaylist(playlistId: Int, trackId: Int): ApiResult<Unit> {
        return try {
            val response = httpClient.post("api/v1/playlists/$playlistId/tracks/add") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("ids" to listOf(trackId)))
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
