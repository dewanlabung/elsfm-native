package com.elsfm.mobile.core.media

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
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayHistoryApiTest {

    private fun clientReturning(status: HttpStatusCode): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond("{}", status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun `recordPlay returns Success on 200`() = runTest {
        val api = PlayHistoryApi(clientReturning(HttpStatusCode.OK))

        val result = api.recordPlay(trackId = 1192)

        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `recordPlay returns NetworkError on 500`() = runTest {
        val api = PlayHistoryApi(clientReturning(HttpStatusCode.InternalServerError))

        val result = api.recordPlay(trackId = 1192)

        assertTrue(result is ApiResult.NetworkError)
    }
}
