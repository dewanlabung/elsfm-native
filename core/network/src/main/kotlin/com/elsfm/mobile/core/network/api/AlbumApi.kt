package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class AlbumResponse(val album: Album)

@Serializable
private data class AlbumTracksPagination(val data: List<Track>)

@Serializable
private data class AlbumTracksResponse(val pagination: AlbumTracksPagination)

class AlbumApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun getAlbum(id: Int): ApiResult<Album> {
        return try {
            val response = httpClient.get("api/v1/albums/$id")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<AlbumResponse>().album)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend fun getAlbumTracks(id: Int): ApiResult<List<Track>> {
        return try {
            val response = httpClient.get("api/v1/albums/$id/tracks")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<AlbumTracksResponse>().pagination.data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
