package com.elsfm.mobile.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LibrarySections(
    val playlists: List<Playlist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val channels: List<Channel> = emptyList(),
)
