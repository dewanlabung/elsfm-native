package com.elsfm.mobile.feature.library

import app.cash.turbine.test
import com.elsfm.mobile.core.network.api.ChannelApi
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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun mockChannelApi(status: HttpStatusCode = HttpStatusCode.OK): ChannelApi {
        val mockEngine = MockEngine { _ ->
            val body = """
                {
                  "channel": {
                    "id": 5,
                    "name": "Nepali Christian Songs",
                    "model_type": "channel",
                    "content": {
                      "data": [
                        {"id": 1, "name": "Sunday School", "model_type": "channel"}
                      ]
                    }
                  }
                }
            """.trimIndent()
            respond(
                body,
                status,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return ChannelApi(httpClient)
    }

    @Test
    fun `loadLibrary updates state with playlists, albums and channels`() = runTest {
        val viewModel = LibraryViewModel(mockChannelApi())

        viewModel.state.test {
            // Initial state (empty)
            assertEquals(LibraryState(), awaitItem())

            // Loading state
            val loadingState = awaitItem()
            assertEquals(true, loadingState.isLoading)

            // Loaded state
            val loadedState = awaitItem()
            assertTrue(loadedState.playlists.isNotEmpty())
            assertTrue(loadedState.albums.isNotEmpty())
            assertEquals(1, loadedState.channels.size)
            assertEquals("Sunday School", loadedState.channels[0].name)
            assertEquals(false, loadedState.isLoading)
            assertNull(loadedState.error)
        }
    }

    @Test
    fun `loadLibrary still shows sample playlists and albums when channels fail`() = runTest {
        val viewModel = LibraryViewModel(mockChannelApi(status = HttpStatusCode.InternalServerError))

        viewModel.state.test {
            assertEquals(LibraryState(), awaitItem())
            awaitItem() // loading

            val loadedState = awaitItem()
            assertTrue(loadedState.playlists.isNotEmpty())
            assertTrue(loadedState.albums.isNotEmpty())
            assertEquals(0, loadedState.channels.size)
            assertEquals(false, loadedState.isLoading)
            assertNotNull(loadedState.error)
        }
    }

    @Test
    fun `selectFilter updates selectedFilter in state`() = runTest {
        val viewModel = LibraryViewModel(mockChannelApi())

        viewModel.selectFilter(LibraryFilter.PLAYLISTS)

        assertEquals(LibraryFilter.PLAYLISTS, viewModel.state.value.selectedFilter)
    }
}
