package com.elsfm.mobile.core.network.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthPluginTest {

    @Test
    fun `attaches bearer token from session manager to requests`() = runTest {
        var capturedAuthHeader: String? = null
        val sessionManager = SessionManager(FakeTokenStore(initial = "secret-token"))
        val mockEngine = MockEngine { request ->
            capturedAuthHeader = request.headers[HttpHeaders.Authorization]
            respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClient(mockEngine) {
            install(AuthPlugin) { this.sessionManager = sessionManager }
        }

        client.get("https://www.elsfm.com/api/v1/tracks")

        assertEquals("Bearer secret-token", capturedAuthHeader)
    }

    @Test
    fun `clears session and does not crash on 401 response`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore(initial = "stale-token"))
        val mockEngine = MockEngine { _ ->
            respond("{}", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClient(mockEngine) {
            install(AuthPlugin) { this.sessionManager = sessionManager }
        }

        client.get("https://www.elsfm.com/api/v1/tracks")

        assertNull(sessionManager.currentToken())
    }
}
