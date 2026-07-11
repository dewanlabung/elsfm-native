package com.elsfm.mobile.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.database.entity.DownloadedTrack
import com.elsfm.mobile.core.database.repository.DownloadRepository
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.feature.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val playerController: PlayerController,
) : ViewModel() {
    private val _state = MutableStateFlow(DownloadsState())
    val state = _state.asStateFlow()

    private var allDownloads: List<DownloadedTrack> = emptyList()

    init {
        loadDownloads()
    }

    private fun loadDownloads() {
        viewModelScope.launch {
            downloadRepository.getCompletedDownloads().collectLatest { downloads ->
                allDownloads = downloads
                updateDownloadsList()
            }
        }
    }

    fun onEvent(event: DownloadsEvent) {
        when (event) {
            is DownloadsEvent.TabChanged -> {
                _state.value = _state.value.copy(activeTab = event.tab)
            }
            is DownloadsEvent.SearchQueryChanged -> {
                _state.value = _state.value.copy(searchQuery = event.query)
                updateDownloadsList()
            }
            is DownloadsEvent.SortChanged -> {
                _state.value = _state.value.copy(sortBy = event.sortBy)
                updateDownloadsList()
            }
            is DownloadsEvent.DeleteDownload -> {
                deleteDownload(event.trackId)
            }
            is DownloadsEvent.ShareDownload -> {
                shareDownload(event.trackId)
            }
            is DownloadsEvent.PlayTrack -> {
                playGroup(allDownloads, startTrackId = event.trackId)
            }
            is DownloadsEvent.PlayAlbum -> {
                playGroup(allDownloads.filter { it.albumId == event.albumId })
            }
            is DownloadsEvent.PlayPlaylist -> {
                playGroup(allDownloads.filter { it.playlistId == event.playlistId })
            }
        }
    }

    private fun updateDownloadsList() {
        val currentState = _state.value
        var filtered = allDownloads

        if (currentState.searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(currentState.searchQuery, ignoreCase = true) ||
                it.artist.contains(currentState.searchQuery, ignoreCase = true)
            }
        }

        filtered = when (currentState.sortBy) {
            SortBy.RECENTLY_ADDED -> filtered.sortedByDescending { it.downloadedAt }
            SortBy.A_TO_Z -> filtered.sortedBy { it.title }
            SortBy.RELEASE_DATE -> filtered.sortedByDescending { it.downloadedAt }
        }

        val tracks = filtered.map { download ->
            DownloadedTrackUI(
                trackId = download.trackId,
                title = download.title,
                artist = download.artist,
                artworkUrl = download.artworkUrl,
                fileSize = formatFileSize(download.fileSizeBytes),
                downloadedAt = download.downloadedAt,
                isOffline = true,
            )
        }

        val albums = filtered
            .filter { it.albumId != null }
            .groupBy { it.albumId }
            .map { (albumId, tracksInAlbum) ->
                DownloadedAlbumUI(
                    albumId = albumId!!,
                    name = tracksInAlbum.first().albumName ?: "Unknown album",
                    artworkUrl = tracksInAlbum.first().artworkUrl,
                    trackCount = tracksInAlbum.size,
                    trackIds = tracksInAlbum.map { it.trackId },
                )
            }

        val playlists = filtered
            .filter { it.playlistId != null }
            .groupBy { it.playlistId }
            .map { (playlistId, tracksInPlaylist) ->
                DownloadedPlaylistUI(
                    playlistId = playlistId!!,
                    name = tracksInPlaylist.first().playlistName ?: "Unknown playlist",
                    artworkUrl = tracksInPlaylist.first().artworkUrl,
                    trackCount = tracksInPlaylist.size,
                    trackIds = tracksInPlaylist.map { it.trackId },
                )
            }

        val files = filtered.map { download ->
            DownloadedFileUI(
                trackId = download.trackId,
                fileName = download.fileName,
                fileSize = formatFileSize(download.fileSizeBytes),
            )
        }

        _state.value = currentState.copy(
            downloadedTracks = tracks,
            downloadedAlbums = albums,
            downloadedPlaylists = playlists,
            downloadedFiles = files,
        )
    }

    private fun deleteDownload(trackId: Int) {
        viewModelScope.launch {
            downloadRepository.deleteDownloadedTrack(trackId)
        }
    }

    private fun shareDownload(trackId: Int) {
        // Share logic will be implemented with actual sharing API
    }

    /**
     * Plays a group of downloaded tracks from their local files, fully offline. When
     * [startTrackId] is given (single-track play from the Songs tab), playback starts on
     * that track with the rest of the downloads as its queue instead of the group's first.
     */
    private fun playGroup(downloads: List<DownloadedTrack>, startTrackId: Int? = null) {
        if (downloads.isEmpty()) return
        val queue = downloads.mapNotNull { download ->
            val file = downloadRepository.getLocalFile(download.fileName) ?: return@mapNotNull null
            Track(
                id = download.trackId,
                name = download.title,
                image = download.artworkUrl,
                durationMs = 0L,
                src = "file://${file.absolutePath}",
                artists = listOf(Artist(id = 0, name = download.artist)),
            )
        }
        if (queue.isEmpty()) return
        val startTrack = startTrackId?.let { id -> queue.find { it.id == id } } ?: queue.first()
        playerController.play(startTrack, queue)
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
            bytes >= 1_000_000 -> "%.2f MB".format(bytes / 1_000_000.0)
            bytes >= 1_000 -> "%.2f KB".format(bytes / 1_000.0)
            else -> "$bytes B"
        }
    }
}
