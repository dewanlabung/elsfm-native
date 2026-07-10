package com.elsfm.mobile.feature.player

data class PlayerMenuState(
    val isMenuVisible: Boolean = false,
    val selectedTrackId: Int? = null,
    val addToPlaylistLoading: Boolean = false,
    val addToLibraryLoading: Boolean = false,
    val repostLoading: Boolean = false,
    val shareLoading: Boolean = false,
    val error: String? = null,
)

sealed class PlayerMenuEvent {
    data class ShowMenu(val trackId: Int) : PlayerMenuEvent()
    data object HideMenu : PlayerMenuEvent()

    /** Purely local: appends the track to the in-memory playback queue. No API call. */
    data class AddToQueue(val trackId: Int) : PlayerMenuEvent()
    data class AddToLibrary(val trackId: Int) : PlayerMenuEvent()
    data class AddToPlaylist(val trackId: Int, val playlistId: Int) : PlayerMenuEvent()
    data class ShareTrack(val trackId: Int) : PlayerMenuEvent()
    data class Repost(val trackId: Int) : PlayerMenuEvent()
}
