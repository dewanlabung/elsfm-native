package com.elsfm.mobile.core.media

import com.elsfm.mobile.core.model.Track
import javax.inject.Inject
import javax.inject.Singleton

private const val DEFAULT_LIMIT = 5

/**
 * Picks the next tracks to auto-play when the queue runs out, sourced
 * entirely from the device's local play history.
 *
 * Strategy: return the most recently played tracks not already in [excludeIds],
 * providing a "more of what you've been listening to" feel with no network call.
 */
@Singleton
class LocalRecommendationEngine @Inject constructor(
    private val recentTracksStore: RecentTracksStore,
) {
    fun getRecommendations(excludeIds: Set<Int>, limit: Int = DEFAULT_LIMIT): List<Track> =
        recentTracksStore.getTracks()
            .filter { it.id !in excludeIds }
            .distinctBy { it.id }
            .take(limit)
}
