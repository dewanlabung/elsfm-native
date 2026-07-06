package com.elsfm.mobile.feature.artist

import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Track

data class ArtistDetailState(
    val artist: Artist? = null,
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val followedByUser: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)
