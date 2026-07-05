package com.elsfm.mobile.core.network.api

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
private data class TrackList(val data: List<Track>)

class ArtistApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun getArtist(id: Int): ApiResult<Artist> {
        return try {
            val response = httpClient.get("api/v1/artist/$id")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<Artist>())
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend fun getArtistTracks(id: Int): ApiResult<List<Track>> {
        return try {
            val response = httpClient.get("api/v1/artist/$id/tracks")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<TrackList>().data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
