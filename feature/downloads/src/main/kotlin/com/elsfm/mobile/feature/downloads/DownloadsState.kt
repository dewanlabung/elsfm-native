package com.elsfm.mobile.feature.downloads

enum class DownloadTab {
    SONGS, ALBUMS, PLAYLISTS
}

enum class SortBy {
    RECENTLY_ADDED, A_TO_Z, RELEASE_DATE
}

data class DownloadedTrackUI(
    val trackId: Int,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val fileSize: String,
    val downloadedAt: Long,
    val isOffline: Boolean = false,
)

data class DownloadedAlbumUI(
    val albumId: Int,
    val name: String,
    val artist: String,
    val artworkUrl: String?,
    val trackCount: Int,
    val trackIds: List<Int>,
)

data class DownloadedPlaylistUI(
    val playlistId: Int,
    val name: String,
    val artworkUrl: String?,
    val trackCount: Int,
    val trackIds: List<Int>,
)

sealed interface DownloadsEvent {
    data class TabChanged(val tab: DownloadTab) : DownloadsEvent
    data class SearchQueryChanged(val query: String) : DownloadsEvent
    data class SortChanged(val sortBy: SortBy) : DownloadsEvent
    data class DeleteDownload(val trackId: Int) : DownloadsEvent
    data class ShareDownload(val trackId: Int) : DownloadsEvent
    data class PlayTrack(val trackId: Int) : DownloadsEvent
    data class PlayAlbum(val albumId: Int) : DownloadsEvent
    data class PlayPlaylist(val playlistId: Int) : DownloadsEvent
    data object PlayAll : DownloadsEvent
    data object ShuffleAll : DownloadsEvent
}

data class DownloadsState(
    val downloadedTracks: List<DownloadedTrackUI> = emptyList(),
    val downloadedAlbums: List<DownloadedAlbumUI> = emptyList(),
    val downloadedPlaylists: List<DownloadedPlaylistUI> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val activeTab: DownloadTab = DownloadTab.SONGS,
    val searchQuery: String = "",
    val sortBy: SortBy = SortBy.RECENTLY_ADDED,
)
