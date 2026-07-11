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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumApiTest {

    private val albumResponseBody = """
        {
          "album": {
            "id": 1,
            "name": "Dark Side",
            "image": "https://example.com/album.jpg",
            "release_date": "2024-01-15",
            "tracks": [
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
    fun `getAlbum returns album and nested tracks with all fields`() = runTest {
        val api = AlbumApi(clientReturning(HttpStatusCode.OK, albumResponseBody))

        val result = api.getAlbum(1)

        assertTrue(result is ApiResult.Success)
        val album = (result as ApiResult.Success).data
        assertEquals(1, album.id)
        assertEquals("Dark Side", album.name)
        assertEquals("https://example.com/album.jpg", album.image)
        assertEquals("2024-01-15", album.releaseDate)

        assertEquals(2, album.tracks.size)
        assertEquals(101, album.tracks[0].id)
        assertEquals("Track 1", album.tracks[0].name)
        assertEquals(180000L, album.tracks[0].durationMs)
        assertNotNull(album.tracks[0].image)
        assertEquals(102, album.tracks[1].id)
        assertEquals("Track 2", album.tracks[1].name)
        assertEquals(240000L, album.tracks[1].durationMs)
    }

    @Test
    fun `getAlbum returns NetworkError on failure`() = runTest {
        val api = AlbumApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.getAlbum(1)

        assertTrue(result is ApiResult.NetworkError)
    }
}
