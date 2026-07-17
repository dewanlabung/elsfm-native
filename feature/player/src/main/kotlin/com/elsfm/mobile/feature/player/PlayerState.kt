package com.elsfm.mobile.feature.player

import com.elsfm.mobile.core.model.Track

enum class PlayerRepeatMode {
    OFF,
    ALL,
    ONE,
}

data class PlayerState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val queue: List<Track> = emptyList(),
    val shuffleEnabled: Boolean = false,
    val repeatMode: PlayerRepeatMode = PlayerRepeatMode.OFF,
    val error: String? = null,
    /** Milliseconds left before the sleep timer pauses playback, or null if none is active. */
    val sleepTimerMillisLeft: Long? = null,
    val playbackSpeed: Float = 1f,
    val volume: Float = 1f,
)
