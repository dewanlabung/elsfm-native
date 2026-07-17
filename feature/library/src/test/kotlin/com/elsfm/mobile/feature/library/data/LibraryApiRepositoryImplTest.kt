package com.elsfm.mobile.feature.library.data

import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.UserEntity
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ChannelApi
import com.elsfm.mobile.core.network.api.PlaylistApi
import com.elsfm.mobile.core.network.api.UserApi
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

private class FakeUserDao(private var cached: UserEntity?) : UserDao {
    override suspend fun upsert(user: UserEntity) {
        cached = user
    }

    override suspend fun get(): UserEntity? = cached

    override suspend fun clear() {
        cached = null
    }
}

class LibraryApiRepositoryImplTest {

    private val userId = 5

    private val userPlaylistsBody = """
        {
          "pagination": {
            "data": [
              {"id": 8, "name": "My Road Trip Mix", "image": null}
            ]
          }
        }
    """.trimIndent()

    private val likedAlbumsBody = """
        {
          "pagination": {
            "data": [
              {"id": 460, "name": "2026 EL Shaddai Youth Camp Songs", "image": null, "release_date": "2026-02-08T00:00:00.000000Z"}
            ]
          }
        }
    """.trimIndent()

    private val likedArtistsBody = """
        {
          "pagination": {
            "data": [
              {"id": 7, "name": "ELShaddai Kalimpong", "image_small": null}
            ]
          }
        }
    """.trimIndent()

    private val homeChannelsBody = """
        {
          "channel": {
            "id": 5,
            "name": "Nepali Christian Songs",
            "model_type": "channel",
            "content": {
              "data": [
                {"id": 24, "name": "Kids Zone", "model_type": "channel"}
              ]
            }
          }
        }
    """.trimIndent()

    private fun buildRepository(
        userPlaylistsStatus: HttpStatusCode = HttpStatusCode.OK,
        likedAlbumsStatus: HttpStatusCode = HttpStatusCode.OK,
        likedArtistsStatus: HttpStatusCode = HttpStatusCode.OK,
        homeChannelsStatus: HttpStatusCode = HttpStatusCode.OK,
        cachedUser: UserEntity? = UserEntity(id = userId, username = "user", name = "User", email = "user@example.com", avatarUrl = null),
    ): LibraryApiRepositoryImpl {
        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/users/$userId/playlists") ->
                    respond(userPlaylistsBody, userPlaylistsStatus, headersOf(HttpHeaders.ContentType, "application/json"))
                path.endsWith("/users/$userId/liked-albums") ->
                    respond(likedAlbumsBody, likedAlbumsStatus, headersOf(HttpHeaders.ContentType, "application/json"))
                path.endsWith("/users/$userId/liked-artists") ->
                    respond(likedArtistsBody, likedArtistsStatus, headersOf(HttpHeaders.ContentType, "application/json"))
                path.endsWith("/channel/5") ->
                    respond(homeChannelsBody, homeChannelsStatus, headersOf(HttpHeaders.ContentType, "application/json"))
                else -> respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return LibraryApiRepositoryImpl(
            playlistApi = PlaylistApi(httpClient),
            userApi = UserApi(httpClient),
            channelApi = ChannelApi(httpClient),
            userDao = FakeUserDao(cachedUser),
        )
    }

    @Test
    fun `loadLibrary returns the signed-in user's playlists, liked albums, liked artists and channels`() = runTest {
        val repository = buildRepository()

        val result = repository.loadLibrary()

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data

        assertEquals(1, data.playlists.size)
        assertEquals("My Road Trip Mix", data.playlists[0].name)

        assertEquals(1, data.albums.size)
        assertEquals("2026 EL Shaddai Youth Camp Songs", data.albums[0].name)

        assertEquals(1, data.artists.size)
        assertEquals("ELShaddai Kalimpong", data.artists[0].name)

        assertEquals(1, data.channels.size)
        assertEquals("Kids Zone", data.channels[0].name)
    }

    @Test
    fun `loadLibrary returns Unauthorized when no user is cached`() = runTest {
        val repository = buildRepository(cachedUser = null)

        val result = repository.loadLibrary()

        assertTrue(result is ApiResult.Unauthorized)
    }

    @Test
    fun `loadLibrary returns NetworkError when the user playlists API fails`() = runTest {
        val repository = buildRepository(userPlaylistsStatus = HttpStatusCode.InternalServerError)

        val result = repository.loadLibrary()

        assertTrue(result is ApiResult.NetworkError)
        assertNotNull((result as ApiResult.NetworkError).cause)
    }

    @Test
    fun `loadLibrary returns NetworkError when the liked albums API fails`() = runTest {
        val repository = buildRepository(likedAlbumsStatus = HttpStatusCode.InternalServerError)

        val result = repository.loadLibrary()

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `loadLibrary returns NetworkError when the liked artists API fails`() = runTest {
        val repository = buildRepository(likedArtistsStatus = HttpStatusCode.InternalServerError)

        val result = repository.loadLibrary()

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `loadLibrary returns NetworkError when the channels API fails`() = runTest {
        val repository = buildRepository(homeChannelsStatus = HttpStatusCode.InternalServerError)

        val result = repository.loadLibrary()

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `createPlaylist returns the new playlist mapped from PlaylistInfo`() = runTest {
        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            if (path.endsWith("/playlists")) {
                respond(
                    """{"playlist": {"id": 99, "name": "Road Trip", "image": null}}""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            } else {
                respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        val repository = LibraryApiRepositoryImpl(
            playlistApi = PlaylistApi(httpClient),
            userApi = UserApi(httpClient),
            channelApi = ChannelApi(httpClient),
            userDao = FakeUserDao(UserEntity(id = userId, username = "user", name = "User", email = "user@example.com", avatarUrl = null)),
        )

        val result = repository.createPlaylist("Road Trip")

        assertTrue(result is ApiResult.Success)
        assertEquals(99, (result as ApiResult.Success).data.id)
        assertEquals("Road Trip", result.data.name)
    }
}
