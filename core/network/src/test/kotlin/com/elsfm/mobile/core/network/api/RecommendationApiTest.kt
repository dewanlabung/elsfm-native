package com.elsfm.mobile.core.network.api

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

class RecommendationApiTest {

    private val responseBody = """
        {
            "data": [
                {"track": {"id": 10, "name": "Rec 1", "duration": 90000, "src": "c.mp3", "image": null, "artists": []}, "score": 0.9},
                {"track": {"id": 11, "name": "Rec 2", "duration": 95000, "src": "d.mp3", "image": null, "artists": []}, "score": 0.8}
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
    fun `getRecommendations returns recommendations`() = runTest {
        val api = RecommendationApi(clientReturning(HttpStatusCode.OK, responseBody))

        val result = api.getRecommendations(basedOn = 1)

        assertTrue(result is ApiResult.Success)
        assertEquals(2, (result as ApiResult.Success).data.size)
    }

    @Test
    fun `getRecommendations returns NetworkError on failure`() = runTest {
        val api = RecommendationApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.getRecommendations(basedOn = 1)

        assertTrue(result is ApiResult.NetworkError)
    }
}
