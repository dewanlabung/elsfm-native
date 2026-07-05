package com.elsfm.mobile.core.network.api

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

class TrendingApiTest {

    private val responseBody = """
        {
            "data": [
                {"track": {"id": 1, "name": "Song 1", "duration": 100000, "src": "a.mp3", "image": null, "artists": []}, "rank": 1},
                {"track": {"id": 2, "name": "Song 2", "duration": 120000, "src": "b.mp3", "image": null, "artists": []}, "rank": 2}
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
    fun `getTrending returns trending tracks`() = runTest {
        val api = TrendingApi(clientReturning(HttpStatusCode.OK, responseBody))

        val result = api.getTrending()

        assertTrue(result is ApiResult.Success)
        assertEquals(2, (result as ApiResult.Success).data.size)
    }

    @Test
    fun `getTrending returns NetworkError on failure`() = runTest {
        val api = TrendingApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.getTrending()

        assertTrue(result is ApiResult.NetworkError)
    }
}
