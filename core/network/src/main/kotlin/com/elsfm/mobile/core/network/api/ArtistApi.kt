package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.ArtistFollower
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
private data class ArtistTracksPagination(val data: List<Track>)

@Serializable
private data class ArtistTracksResponse(val pagination: ArtistTracksPagination)

@Serializable
private data class ArtistAlbumsPagination(
    val data: List<Album>,
    @SerialName("last_page") val lastPage: Int = 1,
    @SerialName("current_page") val currentPage: Int = 1,
)

@Serializable
private data class ArtistAlbumsResponse(val pagination: ArtistAlbumsPagination)

data class ArtistAlbumsPage(
    val albums: List<Album>,
    val currentPage: Int = 1,
    val lastPage: Int = 1,
)

@Serializable
private data class ArtistFollowersPagination(val data: List<ArtistFollower>)

@Serializable
private data class ArtistFollowersResponse(val pagination: ArtistFollowersPagination)

@Serializable
private data class ArtistResponse(val artist: Artist)

open class ArtistApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    open suspend fun getArtist(id: Int): ApiResult<Artist> {
        return try {
            val response = httpClient.get("api/v1/artists/$id")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<ArtistResponse>().artist)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    open suspend fun getArtistTracks(id: Int): ApiResult<List<Track>> {
        return try {
            val response = httpClient.get("api/v1/artists/$id/tracks")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<ArtistTracksResponse>().pagination.data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    open suspend fun getArtistAlbums(id: Int, page: Int = 1): ApiResult<ArtistAlbumsPage> {
        return try {
            val response = httpClient.get("api/v1/artists/$id/albums") {
                parameter("page", page)
            }
            if (response.status.isSuccess()) {
                val pagination = response.body<ArtistAlbumsResponse>().pagination
                ApiResult.Success(
                    ArtistAlbumsPage(
                        albums = pagination.data,
                        currentPage = pagination.currentPage,
                        lastPage = pagination.lastPage,
                    )
                )
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    /**
     * Backed by the real `ArtistFollowersController::index` route:
     * `GET api/v1/artists/{id}/followers`.
     */
    open suspend fun getArtistFollowers(id: Int): ApiResult<List<ArtistFollower>> {
        return try {
            val response = httpClient.get("api/v1/artists/$id/followers")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<ArtistFollowersResponse>().pagination.data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
