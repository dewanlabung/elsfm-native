package com.elsfm.mobile.feature.discovery

import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Playlist

/**
 * Static sample data for Discovery sections that have no backing backend
 * endpoint yet (Featured Playlists, New Releases). Replace with real API
 * calls once `core:network` exposes `List<Playlist>` / `List<Album>`
 * endpoints.
 */
internal object SampleDiscoveryData {
    val featuredPlaylists: List<Playlist> = listOf(
        Playlist(id = 101, name = "Sunday Worship Favorites", image = null),
        Playlist(id = 102, name = "Acoustic Praise", image = null),
        Playlist(id = 103, name = "Youth Group Anthems", image = null),
        Playlist(id = 104, name = "Quiet Reflection", image = null),
    )

    val newReleaseAlbums: List<Album> = listOf(
        Album(id = 201, name = "New Beginnings", image = null, releaseDate = "2026-06-15"),
        Album(id = 202, name = "Rise Up", image = null, releaseDate = "2026-05-30"),
        Album(id = 203, name = "Songs of Grace", image = null, releaseDate = "2026-05-01"),
    )
}
