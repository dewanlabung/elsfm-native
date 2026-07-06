package com.elsfm.mobile.feature.player

data class PlayerMenuState(
    val isMenuVisible: Boolean = false,
    val selectedTrackId: Int? = null,
    val addToPlaylistLoading: Boolean = false,
    val shareLoading: Boolean = false,
    val error: String? = null,
)

sealed class PlayerMenuEvent {
    data class ShowMenu(val trackId: Int) : PlayerMenuEvent()
    data object HideMenu : PlayerMenuEvent()
    data class AddToPlaylist(val trackId: Int, val playlistId: Int) : PlayerMenuEvent()
    data class ShareTrack(val trackId: Int) : PlayerMenuEvent()
}
