package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.elsfmJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelApiTest {

    // Shape confirmed via:
    // curl -H "Accept: application/json" "https://www.elsfm.com/api/v1/channel/5?loader=channelPage"
    // The home channel's content is a mix of nested channels (what we want)
    // and, depending on config, other model types — filtered out by model_type.
    private val responseBody = """
        {
          "channel": {
            "id": 5,
            "slug": "nepali-christiansong",
            "name": "Nepali Christian Songs",
            "model_type": "channel",
            "content": {
              "data": [
                {"id": 24, "slug": "kids-zone", "name": "Kids Zone", "model_type": "channel"},
                {"id": 4, "slug": "mostly-played", "name": "Mostly Played Songs", "model_type": "channel"},
                {"id": 999, "name": "Not a channel", "model_type": "playlist"}
              ]
            }
          }
        }
    """.trimIndent()

    private fun clientReturning(status: HttpStatusCode, body: String): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
    }

    @Test
    fun `getChannels returns list of channels`() = runTest {
        val api = ChannelApi(clientReturning(HttpStatusCode.OK, responseBody))

        val result = api.getChannels()

        assertTrue(result is ApiResult.Success)
        val channels = (result as ApiResult.Success).data
        assertEquals(2, channels.size)
        assertEquals("Kids Zone", channels[0].name)
        assertEquals("Mostly Played Songs", channels[1].name)
    }

    @Test
    fun `getChannels returns NetworkError on failure`() = runTest {
        val api = ChannelApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.getChannels()

        assertTrue(result is ApiResult.NetworkError)
    }

    // Shape confirmed via:
    // curl -H "Accept: application/json" "https://www.elsfm.com/api/v1/channel/4?loader=channelPage"
    private val trackContentBody = """
        {
          "channel": {
            "id": 4,
            "name": "Mostly Played Songs",
            "config": {"contentModel": "track"},
            "content": {
              "data": [
                {"id": 1497, "name": "Khrist Timro Ikchya Lai", "image": null, "duration": 347000, "plays": "1177", "artists": []}
              ]
            }
          }
        }
    """.trimIndent()

    // Shape confirmed via:
    // curl -H "Accept: application/json" "https://www.elsfm.com/api/v1/channel/24?loader=channelPage"
    private val playlistContentBody = """
        {
          "channel": {
            "id": 24,
            "name": "Kids Zone",
            "config": {"contentModel": "playlist"},
            "content": {
              "data": [
                {"id": 8, "name": "All Sunday School Songs", "image": "storage/playlist_media/x.jpeg"}
              ]
            }
          }
        }
    """.trimIndent()

    // Shape confirmed via:
    // curl -H "Accept: application/json" "https://www.elsfm.com/api/v1/channel/1?loader=channelPage"
    private val albumContentBody = """
        {
          "channel": {
            "id": 1,
            "name": "New Release Nepali Christian songs",
            "config": {"contentModel": "album"},
            "content": {
              "data": [
                {"id": 460, "name": "2026 EL Shaddai Youth Camp Songs", "image": null, "release_date": "2026-02-08T00:00:00.000000Z"}
              ]
            }
          }
        }
    """.trimIndent()

    @Test
    fun `getChannelContent returns Tracks when contentModel is track`() = runTest {
        val api = ChannelApi(clientReturning(HttpStatusCode.OK, trackContentBody))

        val result = api.getChannelContent(4)

        assertTrue(result is ApiResult.Success)
        val content = (result as ApiResult.Success).data
        assertTrue(content is ChannelContentResult.Tracks)
        assertEquals(1, (content as ChannelContentResult.Tracks).items.size)
        assertEquals("Khrist Timro Ikchya Lai", content.items[0].name)
    }

    @Test
    fun `getChannelContent returns Playlists when contentModel is playlist`() = runTest {
        val api = ChannelApi(clientReturning(HttpStatusCode.OK, playlistContentBody))

        val result = api.getChannelContent(24)

        assertTrue(result is ApiResult.Success)
        val content = (result as ApiResult.Success).data
        assertTrue(content is ChannelContentResult.Playlists)
        assertEquals(1, (content as ChannelContentResult.Playlists).items.size)
        assertEquals("All Sunday School Songs", content.items[0].name)
    }

    @Test
    fun `getChannelContent returns Albums when contentModel is album`() = runTest {
        val api = ChannelApi(clientReturning(HttpStatusCode.OK, albumContentBody))

        val result = api.getChannelContent(1)

        assertTrue(result is ApiResult.Success)
        val content = (result as ApiResult.Success).data
        assertTrue(content is ChannelContentResult.Albums)
        assertEquals(1, (content as ChannelContentResult.Albums).items.size)
        assertEquals("2026 EL Shaddai Youth Camp Songs", content.items[0].name)
    }

    @Test
    fun `getChannelContent returns NetworkError on failure`() = runTest {
        val api = ChannelApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.getChannelContent(4)

        assertTrue(result is ApiResult.NetworkError)
    }
}
