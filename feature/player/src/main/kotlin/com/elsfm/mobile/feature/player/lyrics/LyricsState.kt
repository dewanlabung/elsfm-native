package com.elsfm.mobile.feature.player.lyrics

import com.elsfm.mobile.core.model.LyricLine

/**
 * UI state for [LyricsScreen], derived from `LyricsApi.getTrackLyrics()`. Plain lyrics have no
 * per-line timestamps; synced lyrics do and drive the highlighted-line behavior.
 */
sealed interface LyricsState {
    data object Loading : LyricsState
    data class PlainLyrics(val lines: List<String>) : LyricsState
    data class SyncedLyrics(val lines: List<LyricLine>) : LyricsState
    data class Error(val message: String) : LyricsState
}
