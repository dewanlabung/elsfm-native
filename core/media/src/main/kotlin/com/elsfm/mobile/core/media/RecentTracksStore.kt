package com.elsfm.mobile.core.media

import android.content.Context
import com.elsfm.mobile.core.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "recent_tracks"
private const val KEY_TRACKS = "tracks"
private const val MAX_HISTORY = 50

@Singleton
class RecentTracksStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private val json = Json { ignoreUnknownKeys = true }

    fun record(track: Track) {
        val current = getTracks().toMutableList()
        current.removeAll { it.id == track.id }
        current.add(0, track)
        prefs.edit().putString(KEY_TRACKS, json.encodeToString(current.take(MAX_HISTORY))).apply()
    }

    fun getTracks(): List<Track> {
        val raw = prefs.getString(KEY_TRACKS, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Track>>(raw) }.getOrDefault(emptyList())
    }
}
