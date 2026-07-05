package com.elsfm.mobile.feature.library

import app.cash.turbine.test
import com.elsfm.mobile.core.network.api.ChannelApi
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

    private fun mockChannelApi(): ChannelApi {
        val mockEngine = MockEngine { _ ->
            val body = """
                {
                  "data": [
                    {"id": 1, "name": "Sunday School"}
                  ]
                }
            """.trimIndent()
            respond(
                body,
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
        return ChannelApi(httpClient)
    }

    @Test
    fun `loadChannels updates state with channels`() = runTest {
        val viewModel = LibraryViewModel(mockChannelApi())

        viewModel.state.test {
            // Initial state (empty)
            assertEquals(LibraryState(), awaitItem())

            // Loading state
            val loadingState = awaitItem()
            assertEquals(true, loadingState.isLoading)
            assertEquals(emptyList<Any>(), loadingState.channels)

            // Loaded state
            val loadedState = awaitItem()
            assertEquals(1, loadedState.channels.size)
            assertEquals("Sunday School", loadedState.channels[0].name)
            assertEquals(false, loadedState.isLoading)
            assertEquals(null, loadedState.error)
        }
    }
}
