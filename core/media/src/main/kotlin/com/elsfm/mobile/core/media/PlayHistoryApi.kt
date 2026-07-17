package com.elsfm.mobile.core.media

import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject

@Serializable
private data class RecordPlayRequest(val track_id: Int, val queue_id: String)

class PlayHistoryApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    private var currentQueueId: String = UUID.randomUUID().toString()

    suspend fun recordPlay(trackId: Int): ApiResult<Unit> {
        return try {
            val response = httpClient.post("api/v1/player/tracks") {
                contentType(ContentType.Application.Json)
                setBody(RecordPlayRequest(track_id = trackId, queue_id = currentQueueId))
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

    fun startNewQueueSession() {
        currentQueueId = UUID.randomUUID().toString()
    }
}
