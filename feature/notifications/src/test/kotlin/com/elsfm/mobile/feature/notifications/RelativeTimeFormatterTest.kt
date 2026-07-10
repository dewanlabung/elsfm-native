package com.elsfm.mobile.feature.notifications

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeFormatterTest {

    private val now = Instant.parse("2024-07-01T00:00:00Z")

    @Test
    fun `formats seconds as just now`() {
        val result = formatRelativeTime("2024-07-01T00:00:00Z", now = now)
        assertEquals("just now", result)
    }

    @Test
    fun `formats minutes ago`() {
        val result = formatRelativeTime("2024-06-30T23:55:00Z", now = now)
        assertEquals("5 minutes ago", result)
    }

    @Test
    fun `formats hours ago`() {
        val result = formatRelativeTime("2024-06-30T20:00:00Z", now = now)
        assertEquals("4 hours ago", result)
    }

    @Test
    fun `formats days ago`() {
        val result = formatRelativeTime("2024-06-28T00:00:00Z", now = now)
        assertEquals("3 days ago", result)
    }

    @Test
    fun `formats months ago`() {
        val result = formatRelativeTime("2024-02-01T00:00:00Z", now = now)
        assertEquals("5 months ago", result)
    }

    @Test
    fun `formats years ago`() {
        val result = formatRelativeTime("2022-07-01T00:00:00Z", now = now)
        assertEquals("2 years ago", result)
    }

    @Test
    fun `returns raw string when timestamp is unparseable`() {
        val result = formatRelativeTime("not-a-date", now = now)
        assertEquals("not-a-date", result)
    }
}
