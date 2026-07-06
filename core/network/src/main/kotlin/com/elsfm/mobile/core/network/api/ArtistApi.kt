package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class ArtistTracksPagination(val data: List<Track>)

@Serializable
private data class ArtistTracksResponse(val pagination: ArtistTracksPagination)

@Serializable
private data class ArtistAlbumsPagination(val data: List<Album>)

@Serializable
private data class ArtistAlbumsResponse(val pagination: ArtistAlbumsPagination)

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

    open suspend fun getArtistAlbums(id: Int): ApiResult<List<Album>> {
        return try {
            val response = httpClient.get("api/v1/artists/$id/albums")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<ArtistAlbumsResponse>().pagination.data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
