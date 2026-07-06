package com.elsfm.mobile.core.network.util

import com.elsfm.mobile.core.network.ApiResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryPolicyTest {

    @Test
    fun `withRetry succeeds on first attempt`() = runTest {
        val result = withRetry {
            ApiResult.Success("data")
        }

        assertTrue(result is ApiResult.Success)
        assertEquals("data", (result as ApiResult.Success).data)
    }

    @Test
    fun `withRetry retries on NetworkError then succeeds`() = runTest {
        var attemptCount = 0

        val result = withRetry {
            attemptCount++
            if (attemptCount < 2) {
                ApiResult.NetworkError(RuntimeException("timeout"))
            } else {
                ApiResult.Success("data")
            }
        }

        assertTrue(result is ApiResult.Success)
        assertEquals("data", (result as ApiResult.Success).data)
        assertEquals(2, attemptCount)
    }

    @Test
    fun `withRetry gives up after maxAttempts exceeded`() = runTest {
        var attemptCount = 0

        val result = withRetry(maxAttempts = 3) {
            attemptCount++
            ApiResult.NetworkError(RuntimeException("timeout"))
        }

        assertTrue(result is ApiResult.NetworkError)
        assertEquals(3, attemptCount)
    }

    @Test
    fun `withRetry does not retry ValidationError`() = runTest {
        var attemptCount = 0

        val result = withRetry {
            attemptCount++
            ApiResult.ValidationError(mapOf("field" to listOf("error")))
        }

        assertTrue(result is ApiResult.ValidationError)
        assertEquals(1, attemptCount)
    }

    @Test
    fun `withRetry does not retry Unauthorized`() = runTest {
        var attemptCount = 0

        val result = withRetry {
            attemptCount++
            ApiResult.Unauthorized
        }

        assertTrue(result is ApiResult.Unauthorized)
        assertEquals(1, attemptCount)
    }

    @Test
    fun `withRetry applies exponential backoff delays`() = runTest {
        val delays = mutableListOf<Long>()
        val startTime = System.currentTimeMillis()

        var attemptCount = 0
        withRetry(maxAttempts = 3, backoff = { attempt ->
            val delay = exponentialBackoff(attempt)
            delays.add(delay)
            delay
        }) {
            attemptCount++
            if (attemptCount < 3) {
                ApiResult.NetworkError(RuntimeException("timeout"))
            } else {
                ApiResult.Success("data")
            }
        }

        // Verify backoff values: 100ms, 200ms
        assertEquals(listOf(100L, 200L), delays)
    }

    @Test
    fun `exponentialBackoff calculates correct delays`() {
        assertEquals(100L, exponentialBackoff(1))
        assertEquals(200L, exponentialBackoff(2))
        assertEquals(400L, exponentialBackoff(3))
    }

    @Test
    fun `withRetry throws on invalid maxAttempts`() = runTest {
        try {
            withRetry(maxAttempts = 0) {
                ApiResult.Success("data")
            }
            assertTrue("Should have thrown", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("maxAttempts must be greater than 0") == true)
        }
    }
}
