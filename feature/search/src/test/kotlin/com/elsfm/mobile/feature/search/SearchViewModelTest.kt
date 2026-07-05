package com.elsfm.mobile.feature.search

import app.cash.turbine.test
import com.elsfm.mobile.core.model.SearchResult
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.SearchApi
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
class SearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun mockSearchApi(): SearchApi {
        val mockEngine = MockEngine { _ ->
            val body = """
                {
                  "data": [
                    {
                      "track": {
                        "id": 100,
                        "name": "Result Track",
                        "duration": 200000,
                        "src": "test.mp3",
                        "image": "test.jpg",
                        "artists": [{"id": 1, "name": "Artist"}]
                      }
                    }
                  ]
                }
            """.trimIndent()
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
        return SearchApi(httpClient)
    }

    @Test
    fun `search updates results`() = runTest {
        val viewModel = SearchViewModel(mockSearchApi())
        viewModel.state.test {
            val initialState = awaitItem()
            assertEquals("", initialState.query)
            assertEquals(emptyList<SearchResult>(), initialState.results)
            assertEquals(false, initialState.isLoading)

            viewModel.search("test")
            val loadingState = awaitItem()
            assertEquals("test", loadingState.query)
            assertEquals(true, loadingState.isLoading)

            val successState = awaitItem()
            assertEquals("test", successState.query)
            assertEquals(1, successState.results.size)
            assertEquals(false, successState.isLoading)
        }
    }
}
