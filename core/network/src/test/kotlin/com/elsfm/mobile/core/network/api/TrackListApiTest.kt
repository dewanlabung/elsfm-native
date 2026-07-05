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

class TrackListApiTest {

    private val responseBody = """
        {
          "playlist": {"id": 8, "name": "All Sunday School Songs"},
          "tracks": {
            "data": [
              {
                "id": 1192,
                "name": "Phul Phulyo Bana Pakhama Ashu Jhajhalkyo",
                "image": "storage/track_image_media/abc.jpeg",
                "duration": 174000,
                "src": "storage/track_media/9cw1dTF9NOayHdT4HdyCydn1jgaVJh7Cm9xu4waT.mp3",
                "artists": [{"id": 30, "name": "Sunday School Songs"}]
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
    fun `getPlaylistTracks parses tracks from the response`() = runTest {
        val api = TrackListApi(clientReturning(HttpStatusCode.OK, responseBody))

        val result = api.getPlaylistTracks(playlistId = 8)

        assertTrue(result is ApiResult.Success)
        val tracks = (result as ApiResult.Success).data
        assertEquals(1, tracks.size)
        assertEquals("Phul Phulyo Bana Pakhama Ashu Jhajhalkyo", tracks[0].name)
    }

    @Test
    fun `getPlaylistTracks returns NetworkError on failure`() = runTest {
        val api = TrackListApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.getPlaylistTracks(playlistId = 8)

        assertTrue(result is ApiResult.NetworkError)
    }
}
