package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.elsfmJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserApiTest {

    private fun clientReturning(status: HttpStatusCode, body: String): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
    }

    private fun clientCapturingRequest(
        status: HttpStatusCode,
        body: String,
        onRequest: (HttpRequestData) -> Unit,
    ): HttpClient {
        val mockEngine = MockEngine { request ->
            onRequest(request)
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
    }

    @Test
    fun `followArtist posts likeables payload to add-to-library endpoint`() = runTest {
        var capturedPath: String? = null
        var capturedBody: String? = null
        val api = UserApi(
            clientCapturingRequest(HttpStatusCode.OK, successBody) { request ->
                capturedPath = request.url.encodedPath
                capturedBody = (request.body as? TextContent)?.text
            },
        )

        val result = api.followArtist(1)

        assertTrue(result is ApiResult.Success)
        assertEquals(true, (result as ApiResult.Success).data)
        assertEquals("/api/v1/users/me/add-to-library", capturedPath)
        assertTrue(capturedBody?.contains("\"likeable_id\":1") == true)
        assertTrue(capturedBody?.contains("\"likeable_type\":\"artist\"") == true)
    }

    @Test
    fun `unfollowArtist posts likeables payload to remove-from-library endpoint`() = runTest {
        var capturedPath: String? = null
        val api = UserApi(
            clientCapturingRequest(HttpStatusCode.OK, successBody) { request ->
                capturedPath = request.url.encodedPath
            },
        )

        val result = api.unfollowArtist(1)

        assertTrue(result is ApiResult.Success)
        assertEquals(false, (result as ApiResult.Success).data)
        assertEquals("/api/v1/users/me/remove-from-library", capturedPath)
    }

    @Test
    fun `followArtist returns NetworkError on exception`() = runTest {
        val mockEngine = MockEngine { _ ->
            throw RuntimeException("Network error")
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        val api = UserApi(client)

        val result = api.followArtist(1)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `unfollowArtist returns NetworkError on exception`() = runTest {
        val mockEngine = MockEngine { _ ->
            throw RuntimeException("Network error")
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        val api = UserApi(client)

        val result = api.unfollowArtist(1)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `followArtist returns NetworkError on server error`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.InternalServerError, ""))

        val result = api.followArtist(1)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `unfollowArtist returns NetworkError on server error`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.InternalServerError, ""))

        val result = api.unfollowArtist(1)

        assertTrue(result is ApiResult.NetworkError)
    }

    private val successBody = """{"status": "success"}"""

    @Test
    fun `addTrackToLibrary posts likeables payload to add-to-library endpoint`() = runTest {
        var capturedPath: String? = null
        var capturedBody: String? = null
        val api = UserApi(
            clientCapturingRequest(HttpStatusCode.OK, successBody) { request ->
                capturedPath = request.url.encodedPath
                capturedBody = (request.body as? TextContent)?.text
            },
        )

        val result = api.addTrackToLibrary(101)

        assertTrue(result is ApiResult.Success)
        assertEquals(true, (result as ApiResult.Success).data)
        assertEquals("/api/v1/users/me/add-to-library", capturedPath)
        assertTrue(capturedBody?.contains("\"likeable_id\":101") == true)
        assertTrue(capturedBody?.contains("\"likeable_type\":\"track\"") == true)
    }

    @Test
    fun `addTrackToLibrary returns NetworkError on server error`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.InternalServerError, ""))

        val result = api.addTrackToLibrary(101)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `addTrackToLibrary returns NetworkError on exception`() = runTest {
        val mockEngine = MockEngine { _ ->
            throw RuntimeException("Network error")
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        val api = UserApi(client)

        val result = api.addTrackToLibrary(101)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `removeTrackFromLibrary posts likeables payload to remove-from-library endpoint`() = runTest {
        var capturedPath: String? = null
        val api = UserApi(
            clientCapturingRequest(HttpStatusCode.OK, successBody) { request ->
                capturedPath = request.url.encodedPath
            },
        )

        val result = api.removeTrackFromLibrary(101)

        assertTrue(result is ApiResult.Success)
        assertEquals(false, (result as ApiResult.Success).data)
        assertEquals("/api/v1/users/me/remove-from-library", capturedPath)
    }

    @Test
    fun `removeTrackFromLibrary returns NetworkError on server error`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.InternalServerError, ""))

        val result = api.removeTrackFromLibrary(101)

        assertTrue(result is ApiResult.NetworkError)
    }

    private val likedTracksResponseBody = """
        {
          "pagination": {
            "data": [
              {
                "id": 42,
                "name": "Liked Track One",
                "image": "storage/track_image_media/liked.jpeg",
                "duration": 210000,
                "plays": "500",
                "artists": [{"id": 7, "name": "Some Artist"}]
              }
            ]
          }
        }
    """.trimIndent()

    @Test
    fun `getLikedTracks parses tracks from the response`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.OK, likedTracksResponseBody))

        val result = api.getLikedTracks(userId = 5)

        assertTrue(result is ApiResult.Success)
        val tracks = (result as ApiResult.Success).data
        assertEquals(1, tracks.size)
        assertEquals("Liked Track One", tracks[0].name)
    }

    @Test
    fun `getLikedTracks requests the user-scoped liked-tracks endpoint`() = runTest {
        var capturedPath: String? = null
        val api = UserApi(
            clientCapturingRequest(HttpStatusCode.OK, likedTracksResponseBody) { request ->
                capturedPath = request.url.encodedPath
            },
        )

        api.getLikedTracks(userId = 5)

        assertEquals("/api/v1/users/5/liked-tracks", capturedPath)
    }

    @Test
    fun `getLikedTracks returns NetworkError on server error`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.InternalServerError, ""))

        val result = api.getLikedTracks(userId = 5)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `getLikedTracks returns NetworkError on exception`() = runTest {
        val mockEngine = MockEngine { _ ->
            throw RuntimeException("Network error")
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        val api = UserApi(client)

        val result = api.getLikedTracks(userId = 5)

        assertTrue(result is ApiResult.NetworkError)
    }

    private val likedAlbumsResponseBody = """
        {
          "pagination": {
            "data": [
              {
                "id": 99,
                "name": "Liked Album One",
                "image": "storage/album_image_media/liked.jpeg",
                "release_date": "2024-01-15"
              }
            ]
          }
        }
    """.trimIndent()

    @Test
    fun `getLikedAlbums parses albums from the response`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.OK, likedAlbumsResponseBody))

        val result = api.getLikedAlbums(userId = 5)

        assertTrue(result is ApiResult.Success)
        val albums = (result as ApiResult.Success).data
        assertEquals(1, albums.size)
        assertEquals("Liked Album One", albums[0].name)
    }

    @Test
    fun `getLikedAlbums requests the user-scoped liked-albums endpoint`() = runTest {
        var capturedPath: String? = null
        val api = UserApi(
            clientCapturingRequest(HttpStatusCode.OK, likedAlbumsResponseBody) { request ->
                capturedPath = request.url.encodedPath
            },
        )

        api.getLikedAlbums(userId = 5)

        assertEquals("/api/v1/users/5/liked-albums", capturedPath)
    }

    @Test
    fun `getLikedAlbums returns NetworkError on server error`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.InternalServerError, ""))

        val result = api.getLikedAlbums(userId = 5)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `getLikedAlbums returns NetworkError on exception`() = runTest {
        val mockEngine = MockEngine { _ ->
            throw RuntimeException("Network error")
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        val api = UserApi(client)

        val result = api.getLikedAlbums(userId = 5)

        assertTrue(result is ApiResult.NetworkError)
    }

    private val likedArtistsResponseBody = """
        {
          "pagination": {
            "data": [
              {"id": 7, "name": "Liked Artist One", "image_small": "storage/artist_image_media/liked.jpeg"}
            ]
          }
        }
    """.trimIndent()

    @Test
    fun `getLikedArtists parses artists from the response`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.OK, likedArtistsResponseBody))

        val result = api.getLikedArtists(userId = 5)

        assertTrue(result is ApiResult.Success)
        val artists = (result as ApiResult.Success).data
        assertEquals(1, artists.size)
        assertEquals("Liked Artist One", artists[0].name)
    }

    @Test
    fun `getLikedArtists requests the user-scoped liked-artists endpoint`() = runTest {
        var capturedPath: String? = null
        val api = UserApi(
            clientCapturingRequest(HttpStatusCode.OK, likedArtistsResponseBody) { request ->
                capturedPath = request.url.encodedPath
            },
        )

        api.getLikedArtists(userId = 5)

        assertEquals("/api/v1/users/5/liked-artists", capturedPath)
    }

    @Test
    fun `getLikedArtists returns NetworkError on server error`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.InternalServerError, ""))

        val result = api.getLikedArtists(userId = 5)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `getLikedArtists returns NetworkError on exception`() = runTest {
        val mockEngine = MockEngine { _ ->
            throw RuntimeException("Network error")
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        val api = UserApi(client)

        val result = api.getLikedArtists(userId = 5)

        assertTrue(result is ApiResult.NetworkError)
    }

    private val followersResponseBody = """
        {
          "pagination": {
            "data": [
              {"id": 8, "name": "Follower One", "username": "follower_one", "image": "storage/user/f.jpeg"}
            ]
          }
        }
    """.trimIndent()

    @Test
    fun `getFollowers parses users from the response`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.OK, followersResponseBody))

        val result = api.getFollowers(userId = 5)

        assertTrue(result is ApiResult.Success)
        val followers = (result as ApiResult.Success).data
        assertEquals(1, followers.size)
        assertEquals("Follower One", followers[0].name)
    }

    @Test
    fun `getFollowers requests the user-scoped followers endpoint`() = runTest {
        var capturedPath: String? = null
        val api = UserApi(
            clientCapturingRequest(HttpStatusCode.OK, followersResponseBody) { request ->
                capturedPath = request.url.encodedPath
            },
        )

        api.getFollowers(userId = 5)

        assertEquals("/api/v1/users/5/followers", capturedPath)
    }

    @Test
    fun `getFollowers returns NetworkError on server error`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.InternalServerError, ""))

        val result = api.getFollowers(userId = 5)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `getFollowers returns NetworkError on exception`() = runTest {
        val mockEngine = MockEngine { _ ->
            throw RuntimeException("Network error")
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        val api = UserApi(client)

        val result = api.getFollowers(userId = 5)

        assertTrue(result is ApiResult.NetworkError)
    }

    private val followedUsersResponseBody = """
        {
          "pagination": {
            "data": [
              {"id": 9, "name": "Followed One", "username": "followed_one", "image": "storage/user/g.jpeg"}
            ]
          }
        }
    """.trimIndent()

    @Test
    fun `getFollowedUsers parses users from the response`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.OK, followedUsersResponseBody))

        val result = api.getFollowedUsers(userId = 5)

        assertTrue(result is ApiResult.Success)
        val followedUsers = (result as ApiResult.Success).data
        assertEquals(1, followedUsers.size)
        assertEquals("Followed One", followedUsers[0].name)
    }

    @Test
    fun `getFollowedUsers requests the user-scoped followed-users endpoint`() = runTest {
        var capturedPath: String? = null
        val api = UserApi(
            clientCapturingRequest(HttpStatusCode.OK, followedUsersResponseBody) { request ->
                capturedPath = request.url.encodedPath
            },
        )

        api.getFollowedUsers(userId = 5)

        assertEquals("/api/v1/users/5/followed-users", capturedPath)
    }

    @Test
    fun `getFollowedUsers returns NetworkError on server error`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.InternalServerError, ""))

        val result = api.getFollowedUsers(userId = 5)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `getFollowedUsers returns NetworkError on exception`() = runTest {
        val mockEngine = MockEngine { _ ->
            throw RuntimeException("Network error")
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        val api = UserApi(client)

        val result = api.getFollowedUsers(userId = 5)

        assertTrue(result is ApiResult.NetworkError)
    }
}
