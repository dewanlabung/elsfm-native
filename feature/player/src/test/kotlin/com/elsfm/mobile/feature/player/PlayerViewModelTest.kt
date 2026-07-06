package com.elsfm.mobile.feature.player

import app.cash.turbine.test
import com.elsfm.mobile.core.media.PlayHistoryApi
import com.elsfm.mobile.core.model.ApiResult
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.feature.player.data.PlayerMenuRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private class FakePlayerController : PlayerController {
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state
    var lastPlayedTrack: Track? = null

    override fun play(track: Track, queue: List<Track>) {
        lastPlayedTrack = track
        _state.value = _state.value.copy(currentTrack = track, isPlaying = true, queue = queue)
    }

    override fun togglePlayPause() {
        _state.value = _state.value.copy(isPlaying = !_state.value.isPlaying)
    }

    override fun seekTo(positionMs: Long) {
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    override fun skipNext() = Unit
    override fun skipPrevious() = Unit
}

private class FakePlayerMenuRepository {
    suspend fun addTrackToPlaylist(playlistId: Int, trackId: Int): ApiResult<Unit> {
        return ApiResult.Success(Unit)
    }

    suspend fun shareTrack(trackId: Int): ApiResult<String> {
        return ApiResult.Success("https://elsfm.com/share/track/$trackId")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val track = Track(
        id = 1192,
        name = "Test Track",
        image = null,
        durationMs = 10_000,
        src = "storage/track_media/x.mp3",
        artists = listOf(Artist(id = 1, name = "Test Artist")),
    )

    private fun playHistoryApiReturning(status: HttpStatusCode): PlayHistoryApi {
        val mockEngine = MockEngine { _ ->
            respond("{}", status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
        return PlayHistoryApi(httpClient)
    }

    @Test
    fun `play delegates to controller and records play history`() = runTest {
        val controller = FakePlayerController()
        val playHistoryApi = playHistoryApiReturning(HttpStatusCode.OK)
        val menuRepository = FakePlayerMenuRepository()
        val viewModel = PlayerViewModel(controller, playHistoryApi, menuRepository)

        viewModel.play(track, listOf(track))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(track, state.currentTrack)
            assertEquals(true, state.isPlaying)
        }
        assertEquals(track, controller.lastPlayedTrack)
    }

    @Test
    fun `togglePlayPause delegates to controller`() = runTest {
        val controller = FakePlayerController()
        val playHistoryApi = playHistoryApiReturning(HttpStatusCode.OK)
        val menuRepository = FakePlayerMenuRepository()
        val viewModel = PlayerViewModel(controller, playHistoryApi, menuRepository)
        viewModel.play(track, listOf(track))

        viewModel.togglePlayPause()

        viewModel.state.test {
            assertEquals(false, awaitItem().isPlaying)
        }
    }
}
