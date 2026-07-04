package com.elsfm.mobile.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

/**
 * Verify that Ktor's Logging plugin at LogLevel.INFO does NOT log Authorization headers,
 * protecting our Sanctum access tokens from leaking into logs.
 */
class LoggingBehaviorTest {

    @Test
    fun `LogLevel INFO does not log Authorization headers`() = runTest {
        val logs = mutableListOf<String>()
        val mockEngine = MockEngine { request ->
            respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }

        val client = HttpClient(mockEngine) {
            install(Logging) {
                level = LogLevel.INFO
                logger = object : Logger {
                    override fun log(message: String) {
                        logs.add(message)
                    }
                }
            }
        }

        client.get("https://api.example.com/test") {
            headers.append(HttpHeaders.Authorization, "Bearer secret-sanctum-token-xyz")
            headers.append("X-Custom-Header", "should-also-not-log")
        }

        // Check that Authorization header is NOT in logs
        val authInLogs = logs.any { it.contains("Authorization") }
        val tokenInLogs = logs.any { it.contains("Bearer secret-sanctum-token") }
        
        Assert.assertFalse("Authorization header should NOT be logged at INFO level", authInLogs)
        Assert.assertFalse("Token value should NOT be logged at INFO level", tokenInLogs)
        
        // INFO level should only log method and URL, not headers
        val methodAndUrlLogged = logs.any { it.contains("GET") && it.contains("api.example.com") }
        Assert.assertTrue("Method and URL should be logged", methodAndUrlLogged)
    }
}
