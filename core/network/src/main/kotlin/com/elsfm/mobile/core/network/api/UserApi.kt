package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
private data class LikeableRequestItem(
    @SerialName("likeable_id") val likeableId: Int,
    @SerialName("likeable_type") val likeableType: String,
)

@Serializable
private data class LikeablesRequest(val likeables: List<LikeableRequestItem>)

@Serializable
private data class LikedTracksPagination(val data: List<Track>)

@Serializable
private data class LikedTracksResponse(val pagination: LikedTracksPagination)

private const val LIKEABLE_TYPE_TRACK = "track"
private const val LIKEABLE_TYPE_ALBUM = "album"
private const val LIKEABLE_TYPE_ARTIST = "artist"

class UserApi @Inject constructor(
    private val httpClient: HttpClient,
) : UserApiLike {
    /**
     * "Following" an artist has no dedicated backend concept - there is no
     * `follows/artists` endpoint. The real mechanism is the same generic
     * likeable library used for track/album likes
     * (`UserLibraryTracksController::addToLibrary`, which accepts
     * `likeable_type: artist` per its validation rule).
     */
    override suspend fun followArtist(artistId: Int): ApiResult<Boolean> =
        postLikeable("api/v1/users/me/add-to-library", artistId, likedResult = true, likeableType = LIKEABLE_TYPE_ARTIST)

    override suspend fun unfollowArtist(artistId: Int): ApiResult<Boolean> =
        postLikeable("api/v1/users/me/remove-from-library", artistId, likedResult = false, likeableType = LIKEABLE_TYPE_ARTIST)

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

    /**
     * Same generic `likeables`-array endpoints as [addTrackToLibrary]/[removeTrackFromLibrary]
     * (`UserLibraryTracksController::addToLibrary`/`removeFromLibrary` accept
     * `likeable_type: track|album|artist` - confirmed via `removeFromLibrary`'s validation
     * rule), just with `likeable_type: "album"`.
     */
    suspend fun addAlbumToLibrary(albumId: Int): ApiResult<Boolean> =
        postLikeable("api/v1/users/me/add-to-library", albumId, likedResult = true, likeableType = LIKEABLE_TYPE_ALBUM)

    suspend fun removeAlbumFromLibrary(albumId: Int): ApiResult<Boolean> =
        postLikeable("api/v1/users/me/remove-from-library", albumId, likedResult = false, likeableType = LIKEABLE_TYPE_ALBUM)

    /**
     * Fetches the full list of tracks the given user has liked ("liked songs").
     *
     * Backed by `GET api/v1/users/{user}/liked-tracks` (`UserLibraryTracksController::index`),
     * route-model-bound on the user's numeric id, ordered by `likes.created_at desc` by
     * default, paginated 30-per-page server-side (same envelope shape as
     * [TrackListApi.getPlaylistTracks]: `{ "pagination": { "data": [...] } }`). Only the
     * first page is requested here — pagination/"load more" is not wired yet.
     */
    suspend fun getLikedTracks(userId: Int): ApiResult<List<Track>> {
        return try {
            val response = httpClient.get("api/v1/users/$userId/liked-tracks")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<LikedTracksResponse>().pagination.data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    private suspend fun postLikeable(
        path: String,
        trackId: Int,
        likedResult: Boolean,
        likeableType: String = LIKEABLE_TYPE_TRACK,
    ): ApiResult<Boolean> {
        return try {
            val response = httpClient.post(path) {
                contentType(ContentType.Application.Json)
                setBody(
                    LikeablesRequest(
                        likeables = listOf(
                            LikeableRequestItem(likeableId = trackId, likeableType = likeableType),
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

    /**
     * Follows another user (e.g. from the artist profile's Followers tab).
     *
     * Backed by the real Laravel `FollowersController::follow` route:
     * `POST api/v1/users/{id}/follow`. This is user-to-user following, distinct from
     * [followArtist] (`api/v1/me/follows/artists/{id}`). The endpoint returns an empty
     * success envelope with no updated-state payload, so the caller must track the new
     * following state optimistically.
     */
    suspend fun followUser(userId: Int): ApiResult<Unit> = postUserFollowAction("follow", userId)

    /**
     * Unfollows another user. Backed by `POST api/v1/users/{id}/unfollow`
     * (`FollowersController::unfollow`).
     */
    suspend fun unfollowUser(userId: Int): ApiResult<Unit> = postUserFollowAction("unfollow", userId)

    private suspend fun postUserFollowAction(action: String, userId: Int): ApiResult<Unit> {
        return try {
            val response = httpClient.post("api/v1/users/$userId/$action")
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
