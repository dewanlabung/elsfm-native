package com.elsfm.mobile.core.network.util

import com.elsfm.mobile.core.network.ApiResult
import kotlinx.coroutines.delay

/**
 * Exponential backoff strategy for retries.
 * Delays: 100ms, 200ms, 400ms for attempts 1, 2, 3
 */
fun exponentialBackoff(attempt: Int): Long {
    return 100L * (1 shl (attempt - 1))
}

/**
 * Retries a suspension function with exponential backoff.
 * Only retries on transient errors (NetworkError), not validation or auth errors.
 *
 * @param maxAttempts Maximum number of attempts (default: 3)
 * @param backoff Function that calculates delay for a given attempt (default: exponential)
 * @param block The suspension function to retry
 * @return The result of the operation, or the last error if all retries fail
 */
suspend inline fun <T> withRetry(
    maxAttempts: Int = 3,
    noinline backoff: (Int) -> Long = ::exponentialBackoff,
    block: suspend () -> ApiResult<T>,
): ApiResult<T> {
    require(maxAttempts > 0) { "maxAttempts must be greater than 0" }

    var lastResult: ApiResult<T>? = null

    for (attempt in 1..maxAttempts) {
        val result = block()

        when (result) {
            is ApiResult.Success -> return result
            is ApiResult.NetworkError -> {
                lastResult = result
                if (attempt < maxAttempts) {
                    delay(backoff(attempt))
                }
            }
            // Don't retry validation or auth errors
            is ApiResult.ValidationError, ApiResult.Unauthorized -> return result
        }
    }

    return lastResult ?: ApiResult.NetworkError(RuntimeException("No result from retries"))
}
