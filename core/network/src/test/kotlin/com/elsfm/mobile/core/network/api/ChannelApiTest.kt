package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.network.ApiResult
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

    private val responseBody = """
        {
          "data": [
            {"id": 1, "name": "Sunday School"},
            {"id": 2, "name": "Bhajans"}
          ]
        }
    """.trimIndent()

    private fun clientReturning(status: HttpStatusCode, body: String): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun `getChannels returns list of channels`() = runTest {
        val api = ChannelApi(clientReturning(HttpStatusCode.OK, responseBody))

        val result = api.getChannels()

        assertTrue(result is ApiResult.Success)
        val channels = (result as ApiResult.Success).data
        assertEquals(2, channels.size)
        assertEquals("Sunday School", channels[0].name)
    }

    @Test
    fun `getChannels returns NetworkError on failure`() = runTest {
        val api = ChannelApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.getChannels()

        assertTrue(result is ApiResult.NetworkError)
    }
}
