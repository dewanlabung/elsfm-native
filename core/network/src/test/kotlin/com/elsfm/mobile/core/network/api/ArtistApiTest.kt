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

class ArtistApiTest {

    // Shape confirmed via: curl -H "Accept: application/json" "https://www.elsfm.com/api/v1/artists/1"
    private val artistResponseBody = """
        {
          "artist": {
            "id": 1,
            "name": "Test Artist",
            "image_small": "storage/artist/x.jpg",
            "verified": true,
            "disabled": false,
            "model_type": "artist",
            "plays": "51961"
          }
        }
    """.trimIndent()

    // Shape confirmed via: curl -H "Accept: application/json" "https://www.elsfm.com/api/v1/artists/1/tracks"
    private val tracksResponseBody = """
        {
          "pagination": {
            "data": [
              {
                "id": 100,
                "name": "Track 1",
                "duration": 200000,
                "plays": "42",
                "image": "test.jpg",
                "artists": [{"id": 1, "name": "Test Artist"}]
              }
            ]
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
