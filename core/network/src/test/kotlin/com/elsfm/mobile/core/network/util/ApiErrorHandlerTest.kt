package com.elsfm.mobile.core.network.util

import com.elsfm.mobile.core.network.ApiResult
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiErrorHandlerTest {

    @Test
    fun `toUserMessage maps NetworkError with timeout`() {
        val result: ApiResult<String> = ApiResult.NetworkError(RuntimeException("timeout"))

        val message = result.toUserMessage()

        assertEquals("Connection timeout. Please try again.", message)
    }

    @Test
    fun `toUserMessage maps NetworkError with connect failure`() {
        val result: ApiResult<String> = ApiResult.NetworkError(RuntimeException("connect failed"))

        val message = result.toUserMessage()

        assertEquals("Connection timeout. Please try again.", message)
    }

    @Test
    fun `toUserMessage maps NetworkError with connection refused`() {
        val result: ApiResult<String> = ApiResult.NetworkError(RuntimeException("Connection refused"))

        val message = result.toUserMessage()

        assertEquals("Connection timeout. Please try again.", message)
    }

    @Test
    fun `toUserMessage maps NetworkError with name not resolved`() {
        val result: ApiResult<String> = ApiResult.NetworkError(RuntimeException("name not resolved"))

        val message = result.toUserMessage()

        assertEquals("Unable to connect. Check your internet connection.", message)
    }

    @Test
    fun `toUserMessage maps NetworkError with unknown host`() {
        val result: ApiResult<String> = ApiResult.NetworkError(RuntimeException("Unknown host"))

        val message = result.toUserMessage()

        assertEquals("Unable to connect. Check your internet connection.", message)
    }

    @Test
    fun `toUserMessage maps generic NetworkError`() {
        val result: ApiResult<String> = ApiResult.NetworkError(RuntimeException("Some network issue"))

        val message = result.toUserMessage()

        assertEquals("Network error occurred. Please try again.", message)
    }

    @Test
    fun `toUserMessage maps ValidationError`() {
        val result: ApiResult<String> = ApiResult.ValidationError(mapOf("field" to listOf("error")))

        val message = result.toUserMessage()

        assertEquals("Invalid request. Please try again.", message)
    }

    @Test
    fun `toUserMessage maps Unauthorized`() {
        val result: ApiResult<String> = ApiResult.Unauthorized

        val message = result.toUserMessage()

        assertEquals("Authentication failed. Please log in again.", message)
    }

    @Test
    fun `toUserMessage returns empty string for Success`() {
        val result: ApiResult<String> = ApiResult.Success("data")

        val message = result.toUserMessage()

        assertEquals("", message)
    }
}
