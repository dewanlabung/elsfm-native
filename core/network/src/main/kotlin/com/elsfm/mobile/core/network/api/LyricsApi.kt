package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.TrackLyrics
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import javax.inject.Inject

/**
 * Backed by the real Laravel `LyricsController::show` route: `GET api/v1/tracks/{id}/lyrics`.
 * Returns a 404 when no lyrics exist for the track and automatic lyric import is disabled
 * server-side, which we surface as [ApiResult.NetworkError] rather than fabricating content.
 */
class LyricsApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun getTrackLyrics(trackId: Int): ApiResult<TrackLyrics> {
        return try {
            val response = httpClient.get("api/v1/tracks/$trackId/lyrics")
            when {
                response.status.isSuccess() -> ApiResult.Success(response.body<TrackLyrics>())
                response.status == HttpStatusCode.NotFound ->
                    ApiResult.NetworkError(RuntimeException("Lyrics not found for track $trackId"))
                else -> ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
