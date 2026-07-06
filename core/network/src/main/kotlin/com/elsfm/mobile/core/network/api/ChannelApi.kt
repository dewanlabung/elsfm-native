package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.elsfmJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
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

// Shape confirmed via:
// curl -H "Accept: application/json" "https://www.elsfm.com/api/v1/channel/{id}?loader=channelPage"
//
// A sub-channel's `config.contentModel` determines what `content.data` holds:
//   - "track"    -> list of Track objects          (e.g. channel 4, "Mostly Played Songs")
//   - "playlist" -> list of Playlist objects        (e.g. channel 24, "Kids Zone")
//   - "album"    -> list of Album objects           (e.g. channel 1, "New Release ... Songs")
//   - "channel"  -> list of nested Channel objects  (e.g. channel 5, the home channel)
private const val CONTENT_MODEL_TRACK = "track"
private const val CONTENT_MODEL_PLAYLIST = "playlist"
private const val CONTENT_MODEL_ALBUM = "album"
private const val CONTENT_MODEL_CHANNEL = "channel"

@Serializable
private data class ChannelContentConfig(val contentModel: String? = null)

@Serializable
private data class ChannelContentPaginationRaw(val data: List<JsonElement> = emptyList())

@Serializable
private data class ChannelContentDetail(
    val config: ChannelContentConfig? = null,
    val content: ChannelContentPaginationRaw? = null,
)

@Serializable
private data class ChannelContentShowResponse(val channel: ChannelContentDetail)

/**
 * Result of fetching a specific channel's own content. Which variant is
 * populated depends on the backend's `config.contentModel` for that channel.
 * Reuses the existing `Track`/`Playlist`/`Album`/`Channel` models from
 * `core:model` rather than introducing parallel duplicate types.
 */
sealed interface ChannelContentResult {
    data class Tracks(val items: List<Track>) : ChannelContentResult
    data class Playlists(val items: List<Playlist>) : ChannelContentResult
    data class Albums(val items: List<Album>) : ChannelContentResult
    data class Channels(val items: List<Channel>) : ChannelContentResult
}

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

    suspend fun getChannelContent(channelId: Int): ApiResult<ChannelContentResult> {
        return try {
            val response = httpClient.get("api/v1/channel/$channelId") {
                parameter("loader", "channelPage")
            }
            if (response.status.isSuccess()) {
                val channel = response.body<ChannelContentShowResponse>().channel
                ApiResult.Success(channel.toChannelContentResult())
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    private fun ChannelContentDetail.toChannelContentResult(): ChannelContentResult {
        val json = elsfmJson()
        val elements = content?.data.orEmpty()
        return when (config?.contentModel) {
            CONTENT_MODEL_TRACK ->
                ChannelContentResult.Tracks(elements.map { json.decodeFromJsonElement(Track.serializer(), it) })
            CONTENT_MODEL_PLAYLIST ->
                ChannelContentResult.Playlists(elements.map { json.decodeFromJsonElement(Playlist.serializer(), it) })
            CONTENT_MODEL_ALBUM ->
                ChannelContentResult.Albums(elements.map { json.decodeFromJsonElement(Album.serializer(), it) })
            else ->
                ChannelContentResult.Channels(elements.map { json.decodeFromJsonElement(Channel.serializer(), it) })
        }
    }
}
