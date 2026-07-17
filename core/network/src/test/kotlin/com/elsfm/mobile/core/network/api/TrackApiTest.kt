package com.elsfm.mobile.core.network.api

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

class TrackApiTest {

    private fun clientReturning(status: HttpStatusCode, body: String): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
    }

    @Test
    fun `getTrack returns the track on success`() = runTest {
        val body = """
            {"loader":"trackPage","track":{"id":1192,"name":"Phul Phulyo Bana Pakhama","image":null,"duration":174000,"artists":[]}}
        """.trimIndent()
        val api = TrackApi(clientReturning(HttpStatusCode.OK, body))

        val result = api.getTrack(1192)

        assertTrue(result is ApiResult.Success)
        assertEquals("Phul Phulyo Bana Pakhama", (result as ApiResult.Success).data.name)
    }

    @Test
    fun `getTrack returns NetworkError on failure`() = runTest {
        val api = TrackApi(clientReturning(HttpStatusCode.NotFound, "{}"))

        val result = api.getTrack(1192)

        assertTrue(result is ApiResult.NetworkError)
    }
}
