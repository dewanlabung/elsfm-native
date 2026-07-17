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

class PlaylistApiTest {

    private val playlistJson = """
        {
            "playlist": {
                "id": 1,
                "name": "Chill Vibes",
                "description": "Relaxing music",
                "image": "https://example.com/image.jpg",
                "tracks_count": 42,
                "updated_at": "2024-01-15"
            }
        }
    """.trimIndent()

    private val userPlaylistsJson = """
        {
            "pagination": {
                "data": [
                    {"id": 1, "name": "Chill Vibes", "image": "https://example.com/image.jpg"},
                    {"id": 2, "name": "Workout Mix", "image": null}
                ]
            }
        }
    """.trimIndent()

    private val tracksJson = """
        {
            "pagination": {
                "data": [
                    {
                        "id": 101,
                        "name": "Track 1",
                        "duration": 180000,
                        "image": "https://example.com/track1.jpg",
                        "artists": []
                    },
                    {
                        "id": 102,
                        "name": "Track 2",
                        "duration": 240000,
                        "image": "https://example.com/track2.jpg",
                        "artists": []
                    }
                ],
                "total": 42,
                "per_page": 50,
                "current_page": 1
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
    fun `getPlaylist returns playlist with all fields`() = runTest {
        val api = PlaylistApi(clientReturning(HttpStatusCode.OK, playlistJson))

        val result = api.getPlaylist(1)

        assertTrue(result is ApiResult.Success)
        val playlist = (result as ApiResult.Success).data
        assertEquals(1, playlist.id)
        assertEquals("Chill Vibes", playlist.name)
        assertEquals(42, playlist.trackCount)
        assertNotNull(playlist.description)
        assertNotNull(playlist.image)
    }

    @Test
    fun `getPlaylist returns NetworkError on failure`() = runTest {
        val api = PlaylistApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.getPlaylist(1)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `getPlaylistTracks returns PaginatedTracks with correct structure`() = runTest {
        val api = PlaylistApi(clientReturning(HttpStatusCode.OK, tracksJson))

        val result = api.getPlaylistTracks(1)

        assertTrue(result is ApiResult.Success)
        val tracks = (result as ApiResult.Success).data
        assertEquals(2, tracks.data.size)
        assertEquals(42, tracks.total)
        assertEquals(50, tracks.perPage)
        assertEquals(1, tracks.currentPage)
        assertEquals("Track 1", tracks.data[0].name)
        assertEquals(101, tracks.data[0].id)
    }

    @Test
    fun `getPlaylistTracks with custom limit parameter`() = runTest {
        val api = PlaylistApi(clientReturning(HttpStatusCode.OK, tracksJson))

        val result = api.getPlaylistTracks(1, limit = 20)

        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `getPlaylistTracks returns NetworkError on failure`() = runTest {
        val api = PlaylistApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.getPlaylistTracks(1)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `getUserPlaylists returns playlists from the real users-playlists endpoint`() = runTest {
        val api = PlaylistApi(clientReturning(HttpStatusCode.OK, userPlaylistsJson))

        val result = api.getUserPlaylists(userId = 5)

        assertTrue(result is ApiResult.Success)
        val playlists = (result as ApiResult.Success).data
        assertEquals(2, playlists.size)
        assertEquals("Chill Vibes", playlists[0].name)
        assertEquals("Workout Mix", playlists[1].name)
    }

    @Test
    fun `getUserPlaylists returns NetworkError on failure`() = runTest {
        val api = PlaylistApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.getUserPlaylists(userId = 5)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `getUserPlaylists normalizes relative image paths to full URLs`() = runTest {
        val body = """
            {"pagination":{"data":[{"id":1,"name":"Chill Vibes","image":"storage/playlist_image_media/abc.jpeg"}]}}
        """.trimIndent()
        val api = PlaylistApi(clientReturning(HttpStatusCode.OK, body))

        val result = api.getUserPlaylists(userId = 5)

        assertTrue(result is ApiResult.Success)
        assertEquals(
            "https://www.elsfm.com/storage/playlist_image_media/abc.jpeg",
            (result as ApiResult.Success).data[0].image,
        )
    }

    @Test
    fun `createPlaylist returns the new playlist on success`() = runTest {
        val api = PlaylistApi(clientReturning(HttpStatusCode.OK, playlistJson))

        val result = api.createPlaylist("Chill Vibes")

        assertTrue(result is ApiResult.Success)
        assertEquals("Chill Vibes", (result as ApiResult.Success).data.name)
    }

    @Test
    fun `createPlaylist returns validation error for a duplicate name`() = runTest {
        val body = """
            {"message":"The given data was invalid.","errors":{"name":["You have already created a playlist with this name."]}}
        """.trimIndent()
        val api = PlaylistApi(clientReturning(HttpStatusCode.UnprocessableEntity, body))

        val result = api.createPlaylist("Chill Vibes")

        assertTrue(result is ApiResult.ValidationError)
        assertEquals(
            listOf("You have already created a playlist with this name."),
            (result as ApiResult.ValidationError).fields["name"],
        )
    }

    @Test
    fun `createPlaylist returns NetworkError on failure`() = runTest {
        val api = PlaylistApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.createPlaylist("Chill Vibes")

        assertTrue(result is ApiResult.NetworkError)
    }
}
