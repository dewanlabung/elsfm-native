package com.elsfm.mobile.core.database.repository

import com.elsfm.mobile.core.common.PersistedPlaybackState
import com.elsfm.mobile.core.common.PlaybackStateStore
import com.elsfm.mobile.core.database.dao.PlaybackStateDao
import com.elsfm.mobile.core.database.entity.PlaybackStateEntity
import com.elsfm.mobile.core.model.Track
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** [PlaybackStateStore] backed by [PlaybackStateDao], see [PlaybackStateStore] for rationale. */
class RoomPlaybackStateStore(
    private val dao: PlaybackStateDao,
) : PlaybackStateStore {

    override suspend fun save(state: PersistedPlaybackState) {
        dao.save(
            PlaybackStateEntity(
                currentTrackJson = Json.encodeToString(state.currentTrack),
                queueJson = Json.encodeToString(state.queue),
                positionMs = state.positionMs,
                speed = state.speed,
                volume = state.volume,
            ),
        )
    }

    override suspend fun restore(): PersistedPlaybackState? {
        val entity = dao.get() ?: return null
        val currentTrack = runCatching { Json.decodeFromString<Track>(entity.currentTrackJson) }.getOrNull() ?: return null
        val queue = runCatching { Json.decodeFromString<List<Track>>(entity.queueJson) }.getOrNull() ?: return null
        return PersistedPlaybackState(
            currentTrack = currentTrack,
            queue = queue,
            positionMs = entity.positionMs,
            speed = entity.speed,
            volume = entity.volume,
        )
    }

    override suspend fun clear() {
        dao.clear()
    }
}
