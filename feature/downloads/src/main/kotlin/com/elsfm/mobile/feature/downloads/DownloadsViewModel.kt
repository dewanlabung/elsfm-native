package com.elsfm.mobile.feature.downloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.database.entity.DownloadedTrack
import com.elsfm.mobile.core.database.repository.DownloadRepository
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.feature.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val playerController: PlayerController,
    @ApplicationContext private val context: Context,
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
            is DownloadsEvent.TabChanged -> _state.value = _state.value.copy(activeTab = event.tab)
            is DownloadsEvent.SearchQueryChanged -> {
                _state.value = _state.value.copy(searchQuery = event.query)
                updateDownloadsList()
            }
            is DownloadsEvent.SortChanged -> {
                _state.value = _state.value.copy(sortBy = event.sortBy)
                updateDownloadsList()
            }
            is DownloadsEvent.DeleteDownload -> deleteDownload(event.trackId)
            is DownloadsEvent.ShareDownload -> shareDownload(event.trackId)
            is DownloadsEvent.PlayTrack -> playGroup(allDownloads, startTrackId = event.trackId)
            is DownloadsEvent.PlayAlbum -> playGroup(allDownloads.filter { it.albumId == event.albumId })
            is DownloadsEvent.PlayPlaylist -> playGroup(allDownloads.filter { it.playlistId == event.playlistId })
            is DownloadsEvent.PlayAll -> playGroup(filteredDownloads())
            is DownloadsEvent.ShuffleAll -> playGroup(filteredDownloads().shuffled())
        }
    }

    private fun filteredDownloads(): List<DownloadedTrack> {
        val q = _state.value.searchQuery
        return if (q.isBlank()) allDownloads
        else allDownloads.filter {
            it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true)
        }
    }

    private fun updateDownloadsList() {
        val currentState = _state.value
        val filtered = filteredDownloads().let { list ->
            when (currentState.sortBy) {
                SortBy.RECENTLY_ADDED -> list.sortedByDescending { it.downloadedAt }
                SortBy.A_TO_Z -> list.sortedBy { it.title }
                SortBy.RELEASE_DATE -> list.sortedByDescending { it.downloadedAt }
            }
        }

        val tracks = filtered.map { d ->
            DownloadedTrackUI(
                trackId = d.trackId,
                title = d.title,
                artist = d.artist,
                artworkUrl = d.artworkUrl,
                fileSize = formatFileSize(d.fileSizeBytes),
                downloadedAt = d.downloadedAt,
                isOffline = true,
            )
        }

        val albums = filtered
            .filter { it.albumId != null }
            .groupBy { it.albumId }
            .map { (albumId, group) ->
                DownloadedAlbumUI(
                    albumId = albumId!!,
                    name = group.first().albumName ?: "Unknown album",
                    artist = group.first().artist,
                    artworkUrl = group.first().artworkUrl,
                    trackCount = group.size,
                    trackIds = group.map { it.trackId },
                )
            }

        val playlists = filtered
            .filter { it.playlistId != null }
            .groupBy { it.playlistId }
            .map { (playlistId, group) ->
                DownloadedPlaylistUI(
                    playlistId = playlistId!!,
                    name = group.first().playlistName ?: "Unknown playlist",
                    artworkUrl = group.first().artworkUrl,
                    trackCount = group.size,
                    trackIds = group.map { it.trackId },
                )
            }

        _state.value = currentState.copy(
            downloadedTracks = tracks,
            downloadedAlbums = albums,
            downloadedPlaylists = playlists,
        )
    }

    private fun deleteDownload(trackId: Int) {
        viewModelScope.launch { downloadRepository.deleteDownloadedTrack(trackId) }
    }

    private fun shareDownload(trackId: Int) {
        val download = allDownloads.find { it.trackId == trackId } ?: return
        val file = downloadRepository.getLocalFile(download.fileName) ?: return
        val uri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            Uri.fromFile(file)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, download.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${download.title}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun playGroup(downloads: List<DownloadedTrack>, startTrackId: Int? = null) {
        if (downloads.isEmpty()) return
        val queue = downloads.mapNotNull { d ->
            val file = downloadRepository.getLocalFile(d.fileName) ?: return@mapNotNull null
            Track(
                id = d.trackId,
                name = d.title,
                image = d.artworkUrl,
                durationMs = 0L,
                src = "file://${file.absolutePath}",
                artists = listOf(Artist(id = 0, name = d.artist)),
            )
        }
        if (queue.isEmpty()) return
        val startTrack = startTrackId?.let { id -> queue.find { it.id == id } } ?: queue.first()
        playerController.play(startTrack, queue)
    }

    private fun formatFileSize(bytes: Long): String = when {
        bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> "%.2f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.2f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}
