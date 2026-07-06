package com.elsfm.mobile.feature.library

import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Playlist

/**
 * Static sample data for the Playlists/Albums grid on [LibraryScreen].
 *
 * `core:network` has no `GET /playlists` or `GET /albums` endpoint that
 * returns the current user's library (only per-channel content and
 * per-playlist tracks exist). Replace with real API calls once such
 * endpoints are added — see `SampleDiscoveryData` in `feature:discovery`
 * for the same pattern applied to the home screen.
 */
internal object SampleLibraryData {
    val playlists: List<Playlist> = listOf(
        Playlist(id = 101, name = "Sunday Worship Favorites", image = null),
        Playlist(id = 102, name = "Acoustic Praise", image = null),
        Playlist(id = 103, name = "Youth Group Anthems", image = null),
        Playlist(id = 104, name = "Quiet Reflection", image = null),
    )

    val albums: List<Album> = listOf(
        Album(id = 201, name = "New Beginnings", image = null, releaseDate = "2026-06-15"),
        Album(id = 202, name = "Rise Up", image = null, releaseDate = "2026-05-30"),
        Album(id = 203, name = "Songs of Grace", image = null, releaseDate = "2026-05-01"),
    )
}
