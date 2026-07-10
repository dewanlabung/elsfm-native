package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Matches the body of `GET api/v1/tracks/{id}/lyrics` (`LyricsController::show` ->
 * `ParseLyric::execute`). Synced lyrics carry a `time` (seconds) per line; unsynced
 * lyrics only carry `text`.
 */
@Serializable
data class TrackLyrics(
    @SerialName("is_synced") val isSynced: Boolean,
    val duration: Int? = null,
    val lines: List<LyricLine> = emptyList(),
)

@Serializable
data class LyricLine(
    val time: Double? = null,
    val text: String,
)
