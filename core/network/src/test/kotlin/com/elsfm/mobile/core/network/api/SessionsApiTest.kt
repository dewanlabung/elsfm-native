package com.elsfm.mobile.core.network.api

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

class SessionsApiTest {

    private val sessionsResponseBody = """
        {
          "sessions": [
            {
              "id": 1,
              "platform": "Android",
              "browser": null,
              "device": "mobile",
              "country": "kr",
              "city": "Seoul",
              "ip_address": "1.2.3.4",
              "is_current_device": true,
              "updated_at": "2024-01-15T10:30:00Z"
            },
            {
              "id": 2,
              "platform": "Windows",
              "browser": "Chrome",
              "device": "desktop",
              "is_current_device": false,
              "updated_at": "2023-08-01T00:00:00Z"
            }
          ]
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
    fun `getUserSessions parses session list`() = runTest {
        val api = SessionsApi(clientReturning(HttpStatusCode.OK, sessionsResponseBody))

        val result = api.getUserSessions()

        assertTrue(result is ApiResult.Success)
        val sessions = (result as ApiResult.Success).data
        assertEquals(2, sessions.size)
        assertEquals(true, sessions[0].isCurrentDevice)
        assertEquals("mobile", sessions[0].device)
        assertEquals("Chrome", sessions[1].browser)
    }

    @Test
    fun `getUserSessions returns Unauthorized on 401`() = runTest {
        val api = SessionsApi(clientReturning(HttpStatusCode.Unauthorized, ""))

        val result = api.getUserSessions()

        assertEquals(ApiResult.Unauthorized, result)
    }

    @Test
    fun `getUserSessions returns NetworkError on server error`() = runTest {
        val api = SessionsApi(clientReturning(HttpStatusCode.InternalServerError, ""))

        val result = api.getUserSessions()

        assertTrue(result is ApiResult.NetworkError)
    }
}
