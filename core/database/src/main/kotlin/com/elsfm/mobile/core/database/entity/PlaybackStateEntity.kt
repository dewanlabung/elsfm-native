package com.elsfm.mobile.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table holding the last-known player state (JSON-encoded [Track][com.elsfm.mobile.core.model.Track]
 * payloads, matching [DownloadedTrack]'s flat-column style but stored as JSON here since the
 * whole queue - not individual scalar fields - needs to round-trip for restore).
 */
@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey
    val id: Int = 0,
    val currentTrackJson: String,
    val queueJson: String,
    val positionMs: Long,
    val speed: Float,
    val volume: Float,
)
