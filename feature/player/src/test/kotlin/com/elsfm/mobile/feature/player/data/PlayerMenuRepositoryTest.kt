package com.elsfm.mobile.feature.player.data

import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.PlaylistApi
import com.elsfm.mobile.core.network.api.ShareTrackResponse
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
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PlayerMenuRepositoryTest {
    private lateinit var repository: PlayerMenuRepository
    private lateinit var playlistApi: PlaylistApi
    private lateinit var userApi: UserApi

    @Before
    fun setup() {
        val mockEngine = MockEngine { request ->
            when {
                request.url.toString().contains("/api/v1/playlists/1/tracks") -> {
                    respond(
                        content = "{}",
                        status = HttpStatusCode.OK,
                        headers = headersOf(Pair("Content-Type", "application/json"))
                    )
                }
                request.url.toString().contains("/api/v1/tracks/1/share") -> {
                    respond(
                        content = """{"share_url": "https://elsfm.com/share/track/1"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(Pair("Content-Type", "application/json"))
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
        repository = PlayerMenuRepository(playlistApi, userApi)
    }

    @Test
    fun testAddTrackToPlaylistSuccess() = runTest {
        val result = repository.addTrackToPlaylist(1, 1)
        assertIs<ApiResult.Success<Unit>>(result)
    }

    @Test
    fun testAddTrackToPlaylistNetworkError() = runTest {
        val result = repository.addTrackToPlaylist(999, 999)
        assertIs<ApiResult.NetworkError>(result)
    }

    @Test
    fun testShareTrackSuccess() = runTest {
        val result = repository.shareTrack(1)
        assertIs<ApiResult.Success<String>>(result)
        assertEquals("https://elsfm.com/share/track/1", (result as ApiResult.Success).data)
    }
}
