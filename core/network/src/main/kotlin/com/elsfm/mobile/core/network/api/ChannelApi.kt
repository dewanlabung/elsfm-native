package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

// The backend's `GET channel` (index/list) endpoint requires the `channels.view`
// permission, which is not part of the default Users/Guests roles (see
// resources/defaults/permissions.php in the Laravel backend) — it 403s for
// ordinary logged-in users. `GET channel/{id}` (show) has no such restriction
// and is what the web frontend actually calls for its homepage
// (see main-*.js bundle: `ua.get(\`channel/${slugOrId}\`, {loader:"channelPage"})`).
//
// Channel id 5 ("Nepali Christian Songs") is the site's homepage channel; its
// `content.data` is a list of nested channels (e.g. "Mostly Played Songs",
// "New Release Nepali Christian songs") that the home screen renders as
// sections. This nested list is what we expose as "channels" here.
private const val HOME_CHANNEL_ID = 5

@Serializable
private data class ChannelContentPagination(val data: List<Channel> = emptyList())

@Serializable
private data class ChannelDetail(val content: ChannelContentPagination? = null)

@Serializable
private data class ChannelShowResponse(val channel: ChannelDetail)

class ChannelApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun getChannels(): ApiResult<List<Channel>> {
        return try {
            val response = httpClient.get("api/v1/channel/$HOME_CHANNEL_ID") {
                parameter("loader", "channelPage")
            }
            if (response.status.isSuccess()) {
                val items = response.body<ChannelShowResponse>().channel.content?.data.orEmpty()
                // The home channel's content mixes model types depending on
                // config.contentModel; only keep genuine nested channels here.
                ApiResult.Success(items.filter { it.modelType == "channel" })
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
