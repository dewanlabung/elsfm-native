package com.elsfm.mobile.feature.notifications

import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Formats an ISO-8601 timestamp (as returned by Laravel's default `created_at` JSON
 * serialization) as a short relative string, e.g. "5 months ago", matching the web
 * client's `FormattedRelativeTime` component used on the notifications list.
 */
fun formatRelativeTime(isoTimestamp: String, now: Instant = Instant.now()): String {
    val then = runCatching { Instant.parse(isoTimestamp) }.getOrNull() ?: return isoTimestamp
    val duration = Duration.between(then, now)
    val seconds = duration.seconds

    if (seconds < 0) return "just now"

    return when {
        seconds < 60 -> "just now"
        seconds < 3600 -> pluralize(seconds / 60, "minute")
        seconds < 86_400 -> pluralize(seconds / 3600, "hour")
        seconds < 2_592_000 -> pluralize(ChronoUnit.DAYS.between(then, now), "day")
        seconds < 31_536_000 -> pluralize(ChronoUnit.DAYS.between(then, now) / 30, "month")
        else -> pluralize(ChronoUnit.DAYS.between(then, now) / 365, "year")
    }
}

private fun pluralize(count: Long, unit: String): String {
    val safeCount = if (count <= 0) 1 else count
    return "$safeCount $unit${if (safeCount == 1L) "" else "s"} ago"
}
