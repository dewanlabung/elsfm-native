package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.SearchResult
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchApiTest {

    private val responseBody = """
        {
          "data": [
            {
              "track": {
                "id": 100,
                "name": "Search Result Track",
                "duration": 200000,
                "src": "test.mp3",
                "image": "test.jpg",
                "artists": [{"id": 1, "name": "Artist"}]
              }
            }
          ]
        }
    """.trimIndent()

    private fun clientReturning(status: HttpStatusCode, body: String): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun `search returns mixed results`() = runTest {
        val api = SearchApi(clientReturning(HttpStatusCode.OK, responseBody))

        val result = api.search("query")

        assertTrue(result is ApiResult.Success)
        val results = (result as ApiResult.Success).data
        assertEquals(1, results.size)
    }

    @Test
    fun `search returns NetworkError on failure`() = runTest {
        val api = SearchApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.search("query")

        assertTrue(result is ApiResult.NetworkError)
    }
}
