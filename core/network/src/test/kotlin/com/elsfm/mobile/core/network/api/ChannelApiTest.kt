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
}
