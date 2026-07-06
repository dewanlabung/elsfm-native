package com.elsfm.mobile.feature.library

import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.network.api.TrackListApi
import com.elsfm.mobile.core.network.elsfmJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
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
import org.junit.Before
import org.junit.Test

internal class FakeDispatcherProvider(dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun mockTrackListApi(status: HttpStatusCode = HttpStatusCode.OK): TrackListApi {
        val mockEngine = MockEngine.create {
            dispatcher = testDispatcher
            addHandler { _ ->
                val body = """
                    {
                      "pagination": {
                        "data": [
                          {"id": 1, "name": "Track 1", "image": null, "duration": 180000, "plays": "12", "artists": []},
                          {"id": 2, "name": "Track 2", "image": null, "duration": 200000, "plays": "34", "artists": []}
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
        return TrackListApi(httpClient)
    }

    private fun viewModel(status: HttpStatusCode = HttpStatusCode.OK) = PlaylistViewModel(
        mockTrackListApi(status),
        FakeDispatcherProvider(testDispatcher),
    )

    @Test
    fun `loadPlaylist populates playlist and tracks on success`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        val playlist = Playlist(id = 42, name = "Test Playlist", image = null)

        viewModel.loadPlaylist(playlist)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(playlist, state.playlist)
        assertEquals(2, state.tracks.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadPlaylist sets error when request fails`() = runTest(testDispatcher) {
        val viewModel = viewModel(status = HttpStatusCode.InternalServerError)
        val playlist = Playlist(id = 42, name = "Test Playlist", image = null)

        viewModel.loadPlaylist(playlist)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(playlist, state.playlist)
        assertEquals(0, state.tracks.size)
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun `deleteTrack removes track from state`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        val playlist = Playlist(id = 42, name = "Test Playlist", image = null)

        viewModel.loadPlaylist(playlist)
        advanceUntilIdle()

        viewModel.deleteTrack(1)

        val state = viewModel.state.value
        assertEquals(1, state.tracks.size)
        assertEquals(2, state.tracks.first().id)
    }
}
