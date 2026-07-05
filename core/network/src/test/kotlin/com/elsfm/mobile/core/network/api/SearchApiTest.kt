package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.SearchResult
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.elsfmJson
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

    // Shape confirmed via: curl -H "Accept: application/json" "https://www.elsfm.com/api/v1/search?query=love"
    private val responseBody = """
        {
          "query": "love",
          "results": {
            "tracks": {
              "data": [
                {
                  "id": 100,
                  "name": "Search Result Track",
                  "duration": 200000,
                  "plays": "42",
                  "image": "test.jpg",
                  "artists": [{"id": 1, "name": "Artist"}]
                }
              ],
              "current_page": 1
            },
            "artists": {
              "data": [
                {"id": 5, "name": "Result Artist", "plays": "10"}
              ],
              "current_page": 1
            },
            "albums": {
              "data": [],
              "current_page": 1
            },
            "playlists": {
              "data": [
                {"id": 9, "name": "Result Playlist"}
              ],
              "current_page": 1
            },
            "users": {
              "data": [],
              "current_page": 1
            }
          },
          "loader": "searchPage"
        }
    """.trimIndent()

    private fun clientReturning(status: HttpStatusCode, body: String): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
    }

    @Test
    fun `search returns mixed results`() = runTest {
        val api = SearchApi(clientReturning(HttpStatusCode.OK, responseBody))

        val result = api.search("love")

        assertTrue(result is ApiResult.Success)
        val results = (result as ApiResult.Success).data
        assertEquals(3, results.size)
        assertTrue(results[0] is SearchResult.TrackResult)
        assertTrue(results[1] is SearchResult.ArtistResult)
        assertTrue(results[2] is SearchResult.PlaylistResult)
    }

    @Test
    fun `search returns NetworkError on failure`() = runTest {
        val api = SearchApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.search("query")

        assertTrue(result is ApiResult.NetworkError)
    }
}
