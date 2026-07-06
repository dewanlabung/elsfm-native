package com.elsfm.mobile.feature.player

import com.elsfm.mobile.core.model.Track
import kotlinx.coroutines.flow.StateFlow

interface PlayerController {
    val state: StateFlow<PlayerState>
    fun play(track: Track, queue: List<Track>)
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun skipNext()
    fun skipPrevious()

    /** Jumps playback directly to [track] within the current queue, if present. */
    fun jumpToQueueItem(track: Track)
}
