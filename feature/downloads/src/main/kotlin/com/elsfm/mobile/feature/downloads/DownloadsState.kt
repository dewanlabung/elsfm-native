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
    val isOffline: Boolean = false
)

sealed interface DownloadsEvent {
    data class TabChanged(val tab: DownloadTab) : DownloadsEvent
    data class SearchQueryChanged(val query: String) : DownloadsEvent
    data class SortChanged(val sortBy: SortBy) : DownloadsEvent
    data class DeleteDownload(val trackId: Int) : DownloadsEvent
    data class ShareDownload(val trackId: Int) : DownloadsEvent
}

data class DownloadsState(
    val downloadedTracks: List<DownloadedTrackUI> = emptyList(),
    val downloadProgress: Map<Int, Float> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val activeTab: DownloadTab = DownloadTab.SONGS,
    val searchQuery: String = "",
    val sortBy: SortBy = SortBy.RECENTLY_ADDED,
)
