package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class ChannelList(val data: List<Channel>)

class ChannelApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun getChannels(): ApiResult<List<Channel>> {
        return try {
            val response = httpClient.get("api/v1/channel")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<ChannelList>().data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
