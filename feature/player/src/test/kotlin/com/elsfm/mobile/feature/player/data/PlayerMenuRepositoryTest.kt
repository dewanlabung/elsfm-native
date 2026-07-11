package com.elsfm.mobile.feature.player.data

import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.PlaylistApi
import com.elsfm.mobile.core.network.api.RepostApi
import com.elsfm.mobile.core.network.api.UserApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlayerMenuRepositoryTest {
    private lateinit var repository: PlayerMenuRepository
    private lateinit var playlistApi: PlaylistApi
    private lateinit var userApi: UserApi
    private lateinit var repostApi: RepostApi

    @Before
    fun setup() {
        val mockEngine = MockEngine { request ->
            when {
                request.url.toString().contains("/api/v1/playlists/1/tracks") -> {
                    respond(
                        content = "{}",
                        status = HttpStatusCode.OK,
                        headers = headersOf(Pair("Content-Type", listOf("application/json")))
                    )
                }
                request.url.toString().contains("/api/v1/users/me/add-to-library") -> {
                    respond(
                        content = "{}",
                        status = HttpStatusCode.OK,
                        headers = headersOf(Pair("Content-Type", listOf("application/json")))
                    )
                }
                request.url.toString().contains("/api/v1/reposts/toggle") -> {
                    respond(
                        content = """{"action": "added"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(Pair("Content-Type", listOf("application/json")))
                    )
                }
                else -> respond("", status = HttpStatusCode.NotFound)
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
        playlistApi = PlaylistApi(httpClient)
        userApi = UserApi(httpClient)
        repostApi = RepostApi(httpClient)
        repository = PlayerMenuRepository(playlistApi, userApi, repostApi)
    }

    @Test
    fun testAddTrackToPlaylistSuccess() = runTest {
        val result = repository.addTrackToPlaylist(1, 1)
        assertTrue(result is ApiResult.Success<Unit>)
    }

    @Test
    fun testAddTrackToPlaylistNetworkError() = runTest {
        val result = repository.addTrackToPlaylist(999, 999)
        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun testAddTrackToLibrarySuccess() = runTest {
        val result = repository.addTrackToLibrary(1)
        assertTrue(result is ApiResult.Success<Boolean>)
        assertEquals(true, (result as ApiResult.Success).data)
    }

    @Test
    fun testRepostTrackSuccess() = runTest {
        val result = repository.repostTrack(1)
        assertTrue(result is ApiResult.Success<String>)
        assertEquals("added", (result as ApiResult.Success).data)
    }
}
