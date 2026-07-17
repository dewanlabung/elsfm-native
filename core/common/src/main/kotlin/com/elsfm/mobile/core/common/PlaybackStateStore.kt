package com.elsfm.mobile.core.common

import com.elsfm.mobile.core.model.Track

/**
 * Persists just enough of the player's state to restore a paused/ready session across app
 * restarts (queue, current track, position, speed, volume). Backed by Room in `core:database`
 * (see `RoomPlaybackStateStore`) rather than DataStore - `core:common` is a plain JVM module
 * with no Android Context available, and `core:database` already owns the app's only
 * persistence layer and Track's existing `kotlinx.serialization` support.
 */
interface PlaybackStateStore {
    suspend fun save(state: PersistedPlaybackState)
    suspend fun restore(): PersistedPlaybackState?
    suspend fun clear()
}

data class PersistedPlaybackState(
    val currentTrack: Track,
    val queue: List<Track>,
    val positionMs: Long,
    val speed: Float,
    val volume: Float,
)
