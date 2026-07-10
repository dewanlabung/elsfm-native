package com.elsfm.mobile.feature.artist

import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.ArtistFollower
import com.elsfm.mobile.core.model.Track

/**
 * Tabs shown on the artist profile screen, matching the real elsfm.com PWA's
 * `ArtistLoader::$artistPageTabs` ordering (Discography, Similar, About, Tracks, Followers).
 * "Followers" is included as a tab here (not a separate drill-down screen) since the backend
 * already models it as one of the artist page tabs.
 */
enum class ArtistTab {
    DISCOGRAPHY,
    SIMILAR_ARTISTS,
    ABOUT,
    TRACKS,
    FOLLOWERS,
}

data class ArtistDetailState(
    val artist: Artist? = null,
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val followedByUser: Boolean = false,
    val isLoading: Boolean = false,
    val isFollowLoading: Boolean = false,
    val selectedTab: ArtistTab = ArtistTab.DISCOGRAPHY,
    /** Loaded lazily on first visit to [ArtistTab.FOLLOWERS] via `GET artists/{id}/followers`. */
    val followers: List<ArtistFollower> = emptyList(),
    val isFollowersLoading: Boolean = false,
    val followersError: String? = null,
    /**
     * User ids the current viewer follows, tracked optimistically since
     * `POST users/{id}/follow` / `unfollow` return no state payload to read back.
     */
    val followedUserIds: Set<Int> = emptySet(),
    val error: String? = null,
)
