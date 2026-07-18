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
private data class TrackDetailResponse(val track: Track)

@Serializable
private data class RelatedTracksPagination(val data: List<Track>)

@Serializable
private data class RelatedTracksResponse(val pagination: RelatedTracksPagination)

/**
 * Real endpoint (`TrackController::show` + `TrackLoader::load`): `GET api/v1/tracks/{id}`.
 * Used for track deep links (`https://www.elsfm.com/track/{id}/{slug}`) - there is no
 * dedicated track-detail screen yet, so a deep link just fetches the track and starts
 * playback.
 */
class TrackApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun getTrack(id: Int): ApiResult<Track> {
        return try {
            val response = httpClient.get("api/v1/tracks/$id")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<TrackDetailResponse>().track)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend fun getRelatedTracks(id: Int): ApiResult<List<Track>> {
        return try {
            val response = httpClient.get("api/v1/tracks?related_to=$id")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<RelatedTracksResponse>().pagination.data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
