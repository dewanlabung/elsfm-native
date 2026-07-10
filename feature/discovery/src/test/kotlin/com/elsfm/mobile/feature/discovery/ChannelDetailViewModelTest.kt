package com.elsfm.mobile.feature.discovery

import androidx.lifecycle.SavedStateHandle
import com.elsfm.mobile.core.network.api.ChannelApi
import com.elsfm.mobile.core.network.api.ChannelContentResult
import com.elsfm.mobile.core.network.elsfmJson
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
class ChannelDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun mockChannelApi(status: HttpStatusCode, body: String): ChannelApi {
        val mockEngine = MockEngine.create {
            dispatcher = testDispatcher
            addHandler { _ ->
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return ChannelApi(httpClient)
    }

    private val playlistChannelBody = """
        {
          "channel": {
            "id": 23,
            "name": "Explore More Channel",
            "config": {"contentModel": "playlist"},
            "content": {
              "data": [
                {"id": 25, "name": "Youth Camp Nepali Christian Songs", "image": null},
                {"id": 26, "name": "Holy Convocation Songs", "image": null}
              ]
            }
          }
        }
    """.trimIndent()

    @Test
    fun `loadChannel success populates playlists content`() = runTest(testDispatcher) {
        val viewModel = ChannelDetailViewModel(
            mockChannelApi(HttpStatusCode.OK, playlistChannelBody),
            SavedStateHandle(mapOf(CHANNEL_DETAIL_CHANNEL_ID_ARG to 23, CHANNEL_DETAIL_TITLE_ARG to "Explore More Channel")),
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Explore More Channel", state.title)
        val content = state.content
        assertTrue(content is ChannelContentResult.Playlists)
        assertEquals(2, (content as ChannelContentResult.Playlists).items.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadChannel failure sets error`() = runTest(testDispatcher) {
        val viewModel = ChannelDetailViewModel(
            mockChannelApi(HttpStatusCode.InternalServerError, playlistChannelBody),
            SavedStateHandle(mapOf(CHANNEL_DETAIL_CHANNEL_ID_ARG to 23)),
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test(expected = IllegalStateException::class)
    fun `missing channelId argument throws`() {
        ChannelDetailViewModel(
            mockChannelApi(HttpStatusCode.OK, playlistChannelBody),
            SavedStateHandle(),
        )
    }
}
