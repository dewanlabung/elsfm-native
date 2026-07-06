package com.elsfm.mobile.feature.player

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elsfm.mobile.core.designsystem.ElsfmTheme
import com.elsfm.mobile.core.media.PlayHistoryApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Hand-written fake, not backed by any real playback engine. Bypasses Hilt/DI
 * entirely so this test does not need @HiltAndroidTest/HiltTestRunner machinery.
 */
private class FakePlayerController : PlayerController {
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state

    override fun play(track: com.elsfm.mobile.core.model.Track, queue: List<com.elsfm.mobile.core.model.Track>) = Unit
    override fun togglePlayPause() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun skipNext() = Unit
    override fun skipPrevious() = Unit
    override fun jumpToQueueItem(track: com.elsfm.mobile.core.model.Track) = Unit
}

@RunWith(AndroidJUnit4::class)
class MiniPlayerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun fakePlayHistoryApi(): PlayHistoryApi {
        val mockEngine = MockEngine { _ ->
            respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
        return PlayHistoryApi(httpClient)
    }

    @Test
    fun miniPlayerIsAbsentWhenNothingIsPlaying() {
        val viewModel = PlayerViewModel(FakePlayerController(), fakePlayHistoryApi())

        composeTestRule.setContent {
            ElsfmTheme {
                MiniPlayer(onExpandClicked = {}, viewModel = viewModel)
            }
        }
        composeTestRule.onAllNodesWithTag("mini_player").assertCountEquals(0)
    }
}
