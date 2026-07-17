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

    /** Appends [track] to the end of the current playback queue. Purely local, no API call. */
    fun addToQueue(track: Track)

    fun toggleShuffle()

    /** Cycles OFF -> ALL -> ONE -> OFF. */
    fun cycleRepeatMode()

    /**
     * Stops playback and clears the queue entirely (not just pause) - used on logout so
     * a track already playing doesn't keep playing under the next signed-out/different
     * session.
     */
    fun stop()

    /** Pauses playback after [minutes]; replaces any timer already running. */
    fun startSleepTimer(minutes: Int)

    /** Cancels a running sleep timer, if any. */
    fun cancelSleepTimer()

    /** Clamped to 0.5x-2x. */
    fun setPlaybackSpeed(speed: Float)

    /** In-app software volume, clamped to 0f-1f, independent of the hardware volume keys. */
    fun setVolume(volume: Float)

    /** Restores a persisted queue/track/position in a paused (not auto-playing) state, if one exists. */
    suspend fun restorePersistedState()
}
