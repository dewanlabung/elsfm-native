package com.elsfm.mobile.feature.library

import androidx.lifecycle.SavedStateHandle
import com.elsfm.mobile.core.network.api.AlbumApi
import com.elsfm.mobile.core.network.api.UserApi
import com.elsfm.mobile.core.network.elsfmJson
import com.elsfm.mobile.feature.library.data.TrackLikeController
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun mockAlbumApi(status: HttpStatusCode = HttpStatusCode.OK): AlbumApi {
        val mockEngine = MockEngine.create {
            dispatcher = testDispatcher
            addHandler { _ ->
                val body = """
                    {
                      "album": {
                        "id": 7,
                        "name": "Test Album",
                        "image": null,
                        "release_date": "2026-01-01",
                        "tracks": [
                          {"id": 1, "name": "Track 1", "image": null, "duration": 180000, "plays": "12", "artists": []}
                        ]
                      }
                    }
                """.trimIndent()
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return AlbumApi(httpClient)
    }

    private fun mockUserApi(status: HttpStatusCode = HttpStatusCode.OK): UserApi {
        val mockEngine = MockEngine.create {
            dispatcher = testDispatcher
            addHandler { _ ->
                respond("{}", status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return UserApi(httpClient)
    }

    private fun viewModel(
        status: HttpStatusCode = HttpStatusCode.OK,
        likeStatus: HttpStatusCode = HttpStatusCode.OK,
    ) = AlbumViewModel(
        SavedStateHandle(mapOf(ALBUM_ID_ARG to 7)),
        mockAlbumApi(status),
        TrackLikeController(mockUserApi(likeStatus)),
        FakeDispatcherProvider(testDispatcher),
    )

    @Test
    fun `loadAlbum populates album and tracks on success`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(7, state.album?.id)
        assertEquals(1, state.tracks.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadAlbum sets error when request fails`() = runTest(testDispatcher) {
        val viewModel = viewModel(status = HttpStatusCode.InternalServerError)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.album)
        assertEquals(0, state.tracks.size)
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun `toggleTrackLike adds track to likedTrackIds on success`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.toggleTrackLike(1)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.likedTrackIds.contains(1))
        assertFalse(state.likeLoadingTrackIds.contains(1))
    }

    @Test
    fun `toggleTrackLike sets error on failure without changing liked state`() = runTest(testDispatcher) {
        val viewModel = viewModel(likeStatus = HttpStatusCode.InternalServerError)
        advanceUntilIdle()

        viewModel.toggleTrackLike(1)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.likedTrackIds.contains(1))
        assertNotNull(state.error)
    }
}
