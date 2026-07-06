package com.elsfm.mobile.core.network.util

import com.elsfm.mobile.core.network.ApiResult

/**
 * Converts API errors to user-friendly error messages.
 *
 * Maps different error types to appropriate messages for display in the UI.
 */
fun ApiResult<*>.toUserMessage(): String {
    return when (this) {
        is ApiResult.Success -> "" // No error message for success
        is ApiResult.NetworkError -> {
            val message = cause.message.orEmpty().lowercase()
            when {
                message.contains("timeout", ignoreCase = true) ||
                message.contains("connect", ignoreCase = true) ||
                message.contains("connection refused", ignoreCase = true) -> {
                    "Connection timeout. Please try again."
                }
                message.contains("name not resolved", ignoreCase = true) ||
                message.contains("unknown host", ignoreCase = true) -> {
                    "Unable to connect. Check your internet connection."
                }
                else -> "Network error occurred. Please try again."
            }
        }
        is ApiResult.ValidationError -> "Invalid request. Please try again."
        is ApiResult.Unauthorized -> "Authentication failed. Please log in again."
    }
}
