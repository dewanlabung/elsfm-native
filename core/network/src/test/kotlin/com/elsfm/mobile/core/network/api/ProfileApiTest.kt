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

class ProfileApiTest {

    private val responseBody = """
        {
            "id": 1,
            "name": "Jane Doe",
            "email": "jane@example.com",
            "image": null,
            "bio": "Music lover",
            "followers_count": 5,
            "followed_count": 3
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
    fun `getProfile returns user profile`() = runTest {
        val api = ProfileApi(clientReturning(HttpStatusCode.OK, responseBody))

        val result = api.getProfile()

        assertTrue(result is ApiResult.Success)
        assertEquals("Jane Doe", (result as ApiResult.Success).data.name)
    }

    @Test
    fun `getProfile returns NetworkError on failure`() = runTest {
        val api = ProfileApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.getProfile()

        assertTrue(result is ApiResult.NetworkError)
    }

    private val recentlyPlayedResponseBody = """
        {
          "pagination": {
            "data": [
              {"id": 1, "name": "Track 1", "image": null, "duration": 180000, "plays": "12", "artists": []}
            ]
          }
        }
    """.trimIndent()

    @Test
    fun `getRecentlyPlayed returns tracks from the real tracks-plays-me endpoint`() = runTest {
        val api = ProfileApi(clientReturning(HttpStatusCode.OK, recentlyPlayedResponseBody))

        val result = api.getRecentlyPlayed()

        assertTrue(result is ApiResult.Success)
        val tracks = (result as ApiResult.Success).data
        assertEquals(1, tracks.size)
        assertEquals("Track 1", tracks[0].name)
    }

    @Test
    fun `getRecentlyPlayed returns NetworkError on failure`() = runTest {
        val api = ProfileApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.getRecentlyPlayed()

        assertTrue(result is ApiResult.NetworkError)
    }
}
