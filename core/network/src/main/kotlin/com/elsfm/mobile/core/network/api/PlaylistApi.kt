package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.ImageUrlSerializer
import com.elsfm.mobile.core.model.LaravelValidationError
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
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
    @Serializable(with = ImageUrlSerializer::class)
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

@Serializable
private data class CreatePlaylistRequest(val name: String)

@Serializable
private data class TrackIdsRequest(val ids: List<Int>)

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
     * Real endpoint (`PlaylistController::store`): `POST api/v1/playlists`. Only `name`
     * is required (`ModifyPlaylist` validation - min 3, max 100 chars, unique per owner).
     */
    suspend fun createPlaylist(name: String): ApiResult<PlaylistInfo> {
        return try {
            val response = httpClient.post("api/v1/playlists") {
                contentType(ContentType.Application.Json)
                setBody(CreatePlaylistRequest(name))
            }
            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.Created ->
                    ApiResult.Success(response.body<PlaylistInfoDetail>().playlist)
                HttpStatusCode.UnprocessableEntity -> {
                    val error = response.body<LaravelValidationError>()
                    ApiResult.ValidationError(error.errors)
                }
                else -> ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
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
                setBody(TrackIdsRequest(listOf(trackId)))
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

    /**
     * Real endpoint is `POST api/v1/playlists/{id}/tracks/remove`
     * (`PlaylistTracksController::remove`), same `ids` array shape as add.
     */
    suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: Int): ApiResult<Unit> {
        return try {
            val response = httpClient.post("api/v1/playlists/$playlistId/tracks/remove") {
                contentType(ContentType.Application.Json)
                setBody(TrackIdsRequest(listOf(trackId)))
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

    /**
     * Real endpoint (`PlaylistController::update`): `PUT api/v1/playlists/{id}`. Allowed
     * for the playlist owner/editor (`PlaylistPolicy::update`) without any special
     * permission grant.
     */
    suspend fun updatePlaylist(id: Int, name: String): ApiResult<PlaylistInfo> {
        return try {
            val response = httpClient.put("api/v1/playlists/$id") {
                contentType(ContentType.Application.Json)
                setBody(CreatePlaylistRequest(name))
            }
            when {
                response.status.isSuccess() ->
                    ApiResult.Success(response.body<PlaylistInfoDetail>().playlist)
                response.status == HttpStatusCode.UnprocessableEntity -> {
                    val error = response.body<LaravelValidationError>()
                    ApiResult.ValidationError(error.errors)
                }
                else -> ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    /**
     * Real endpoint (`PlaylistController::destroy`): `DELETE api/v1/playlists/{id}`.
     * Allowed for the playlist owner/editor (`PlaylistPolicy::destroy`) without any
     * special permission grant.
     */
    suspend fun deletePlaylist(id: Int): ApiResult<Unit> {
        return try {
            val response = httpClient.delete("api/v1/playlists/$id")
            if (response.status.isSuccess()) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    /** Real endpoint (`PlaylistController::follow`): `POST api/v1/playlists/{id}/follow`. */
    suspend fun followPlaylist(id: Int): ApiResult<Unit> {
        return try {
            val response = httpClient.post("api/v1/playlists/$id/follow")
            if (response.status.isSuccess()) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    /** Real endpoint (`PlaylistController::unfollow`): `POST api/v1/playlists/{id}/unfollow`. */
    suspend fun unfollowPlaylist(id: Int): ApiResult<Unit> {
        return try {
            val response = httpClient.post("api/v1/playlists/$id/unfollow")
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
