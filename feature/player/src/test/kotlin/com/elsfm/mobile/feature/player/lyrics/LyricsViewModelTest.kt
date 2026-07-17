package com.elsfm.mobile.feature.player.lyrics

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.elsfm.mobile.core.network.api.LyricsApi
import com.elsfm.mobile.feature.player.PlayerController
import com.elsfm.mobile.feature.player.PlayerState
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import com.elsfm.mobile.core.model.Track
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakePlayerController : PlayerController {
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state

    fun setPositionMs(positionMs: Long) {
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    override fun play(track: Track, queue: List<Track>) = Unit
    override fun togglePlayPause() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun skipNext() = Unit
    override fun skipPrevious() = Unit
    override fun jumpToQueueItem(track: Track) = Unit
    override fun addToQueue(track: Track) = Unit
    override fun toggleShuffle() = Unit
    override fun cycleRepeatMode() = Unit
    override fun stop() = Unit
    override fun startSleepTimer(minutes: Int) = Unit
    override fun cancelSleepTimer() = Unit
    override fun setPlaybackSpeed(speed: Float) = Unit
    override fun setVolume(volume: Float) = Unit
    override suspend fun restorePersistedState() = Unit
}

private fun lyricsApiReturning(status: HttpStatusCode, body: String): LyricsApi {
    val mockEngine = MockEngine { _ ->
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    val httpClient = HttpClient(mockEngine) {
        install(ContentNegotiation) { json() }
    }
    return LyricsApi(httpClient)
}

private val syncedLyricsBody = """
    {
      "is_synced": true,
      "duration": 180,
      "lines": [
        {"time": 0.0, "text": "First line"},
        {"time": 5.5, "text": "Second line"}
      ]
    }
""".trimIndent()

private val plainLyricsBody = """
    {
      "is_synced": false,
      "lines": [
        {"text": "First line"},
        {"text": "Second line"}
      ]
    }
""".trimIndent()

private fun buildViewModel(
    lyricsApi: LyricsApi,
    playerController: PlayerController = FakePlayerController(),
    trackId: Int = 5,
) = LyricsViewModel(
    lyricsApi = lyricsApi,
    playerController = playerController,
    savedStateHandle = SavedStateHandle().apply { set(TRACK_ID_ARG, trackId) },
)

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads synced lyrics successfully on init`() = runTest {
        val viewModel = buildViewModel(lyricsApiReturning(HttpStatusCode.OK, syncedLyricsBody))

        viewModel.state.test {
            assertEquals(LyricsState.Loading, awaitItem())

            val loadedState = awaitItem()
            assertTrue(loadedState is LyricsState.SyncedLyrics)
            assertEquals(2, (loadedState as LyricsState.SyncedLyrics).lines.size)
            assertEquals("First line", loadedState.lines[0].text)
        }
    }

    @Test
    fun `loads plain lyrics successfully on init`() = runTest {
        val viewModel = buildViewModel(lyricsApiReturning(HttpStatusCode.OK, plainLyricsBody))

        viewModel.state.test {
            assertEquals(LyricsState.Loading, awaitItem())

            val loadedState = awaitItem()
            assertTrue(loadedState is LyricsState.PlainLyrics)
            assertEquals(listOf("First line", "Second line"), (loadedState as LyricsState.PlainLyrics).lines)
        }
    }

    @Test
    fun `shows error when lyrics fetch fails`() = runTest {
        val viewModel = buildViewModel(lyricsApiReturning(HttpStatusCode.NotFound, ""))

        viewModel.state.test {
            assertEquals(LyricsState.Loading, awaitItem())

            val errorState = awaitItem()
            assertTrue(errorState is LyricsState.Error)
        }
    }

    @Test
    fun `retryLoad reloads lyrics`() = runTest {
        val viewModel = buildViewModel(lyricsApiReturning(HttpStatusCode.OK, syncedLyricsBody))

        viewModel.state.test {
            awaitItem() // initial loading
            awaitItem() // loaded

            viewModel.retryLoad()

            val loadingState = awaitItem()
            assertEquals(LyricsState.Loading, loadingState)

            val reloadedState = awaitItem()
            assertTrue(reloadedState is LyricsState.SyncedLyrics)
        }
    }

    @Test
    fun `positionMs reflects the player controller's live position`() = runTest {
        val controller = FakePlayerController()
        val viewModel = buildViewModel(
            lyricsApi = lyricsApiReturning(HttpStatusCode.OK, syncedLyricsBody),
            playerController = controller,
        )

        controller.setPositionMs(4200)

        viewModel.positionMs.test {
            assertEquals(0L, awaitItem())
            assertEquals(4200L, awaitItem())
        }
    }
}
