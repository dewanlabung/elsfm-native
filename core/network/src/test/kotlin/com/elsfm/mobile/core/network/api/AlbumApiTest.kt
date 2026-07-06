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

class AlbumApiTest {

    private val albumResponseBody = """
        {
          "album": {
            "id": 1,
            "name": "Dark Side",
            "image": "https://example.com/album.jpg",
            "release_date": "2024-01-15"
          }
        }
    """.trimIndent()

    private val tracksResponseBody = """
        {
          "pagination": {
            "data": [
              {
                "id": 101,
                "name": "Track 1",
                "duration": 180000,
                "image": "test.jpg",
                "artists": [{"id": 1, "name": "Test Artist"}]
              },
              {
                "id": 102,
                "name": "Track 2",
                "duration": 240000,
                "image": "test2.jpg",
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
    fun `getAlbum returns album detail`() = runTest {
        val api = AlbumApi(clientReturning(HttpStatusCode.OK, albumResponseBody))

        val result = api.getAlbum(1)

        assertTrue(result is ApiResult.Success)
        val album = (result as ApiResult.Success).data
        assertEquals("Dark Side", album.name)
    }

    @Test
    fun `getAlbumTracks returns list of tracks`() = runTest {
        val api = AlbumApi(clientReturning(HttpStatusCode.OK, tracksResponseBody))

        val result = api.getAlbumTracks(1)

        assertTrue(result is ApiResult.Success)
        val tracks = (result as ApiResult.Success).data
        assertEquals(2, tracks.size)
        assertEquals("Track 1", tracks[0].name)
    }
}
