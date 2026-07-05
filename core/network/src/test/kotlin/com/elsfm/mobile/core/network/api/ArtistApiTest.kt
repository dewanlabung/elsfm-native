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

class ArtistApiTest {

    private val artistResponseBody = """
        {
          "id": 1,
          "name": "Test Artist"
        }
    """.trimIndent()

    private val tracksResponseBody = """
        {
          "data": [
            {
              "id": 100,
              "name": "Track 1",
              "duration": 200000,
              "src": "test1.mp3",
              "image": "test.jpg",
              "artists": [{"id": 1, "name": "Test Artist"}]
            }
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
    fun `getArtist returns artist detail`() = runTest {
        val api = ArtistApi(clientReturning(HttpStatusCode.OK, artistResponseBody))

        val result = api.getArtist(1)

        assertTrue(result is ApiResult.Success)
        val artist = (result as ApiResult.Success).data
        assertEquals("Test Artist", artist.name)
    }

    @Test
    fun `getArtistTracks returns list of tracks`() = runTest {
        val api = ArtistApi(clientReturning(HttpStatusCode.OK, tracksResponseBody))

        val result = api.getArtistTracks(1)

        assertTrue(result is ApiResult.Success)
        val tracks = (result as ApiResult.Success).data
        assertEquals(1, tracks.size)
    }
}
