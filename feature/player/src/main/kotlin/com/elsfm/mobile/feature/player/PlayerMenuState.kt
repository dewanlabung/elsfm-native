package com.elsfm.mobile.feature.player

import com.elsfm.mobile.core.network.api.PlaylistInfo

data class PlayerMenuState(
    val isMenuVisible: Boolean = false,
    val selectedTrackId: Int? = null,
    val addToPlaylistLoading: Boolean = false,
    val addToLibraryLoading: Boolean = false,
    val repostLoading: Boolean = false,
    val isLiked: Boolean = false,
    val isLikeLoading: Boolean = false,
    val isPlaylistPickerVisible: Boolean = false,
    val isLoadingPlaylists: Boolean = false,
    val userPlaylists: List<PlaylistInfo> = emptyList(),
    val downloadingTrackIds: Set<Int> = emptySet(),
    val downloadedTrackIds: Set<Int> = emptySet(),
    val error: String? = null,
)

sealed class PlayerMenuEvent {
    data class ShowMenu(val trackId: Int) : PlayerMenuEvent()
    data object HideMenu : PlayerMenuEvent()

    /** Purely local: appends the track to the in-memory playback queue. No API call. */
    data class AddToQueue(val trackId: Int) : PlayerMenuEvent()
    data class AddToLibrary(val trackId: Int) : PlayerMenuEvent()
    data class ShowPlaylistPicker(val trackId: Int) : PlayerMenuEvent()
    data object HidePlaylistPicker : PlayerMenuEvent()
    data class AddToPlaylist(val trackId: Int, val playlistId: Int) : PlayerMenuEvent()
    data class Repost(val trackId: Int) : PlayerMenuEvent()
    data class MakeAvailableOffline(val trackId: Int) : PlayerMenuEvent()
    data class PlayNext(val trackId: Int) : PlayerMenuEvent()
}
