package com.elsfm.mobile.feature.library.data

import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.model.Playlist

/**
 * Combined library data (playlists, albums, artists, channels) returned by [LibraryApiRepository].
 */
data class LibraryData(
    val playlists: List<Playlist>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val channels: List<Channel>,
)
