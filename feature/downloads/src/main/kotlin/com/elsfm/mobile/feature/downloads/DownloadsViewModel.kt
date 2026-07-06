package com.elsfm.mobile.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.database.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {
    private val _state = MutableStateFlow(DownloadsState())
    val state = _state.asStateFlow()

    init {
        loadDownloads()
    }

    private fun loadDownloads() {
        viewModelScope.launch {
            downloadRepository.getCompletedDownloads().collectLatest { downloads ->
                val tracks = downloads.map { download ->
                    DownloadedTrackUI(
                        trackId = download.trackId,
                        title = download.title,
                        artist = download.artist,
                        artworkUrl = download.artworkUrl,
                        fileSize = formatFileSize(download.fileSizeBytes),
                        downloadedAt = download.downloadedAt,
                        isOffline = true
                    )
                }
                updateDownloadsList(tracks)
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
            }
            is DownloadsEvent.SortChanged -> {
                _state.value = _state.value.copy(sortBy = event.sortBy)
            }
            is DownloadsEvent.DeleteDownload -> {
                deleteDownload(event.trackId)
            }
            is DownloadsEvent.ShareDownload -> {
                shareDownload(event.trackId)
            }
        }
    }

    private fun updateDownloadsList(tracks: List<DownloadedTrackUI>) {
        val currentState = _state.value
        var filtered = tracks

        // Filter by search query
        if (currentState.searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(currentState.searchQuery, ignoreCase = true) ||
                it.artist.contains(currentState.searchQuery, ignoreCase = true)
            }
        }

        // Sort
        filtered = when (currentState.sortBy) {
            SortBy.RECENTLY_ADDED -> filtered.sortedByDescending { it.downloadedAt }
            SortBy.A_TO_Z -> filtered.sortedBy { it.title }
            SortBy.RELEASE_DATE -> filtered.sortedByDescending { it.downloadedAt }
        }

        _state.value = currentState.copy(downloadedTracks = filtered)
    }

    private fun deleteDownload(trackId: Int) {
        viewModelScope.launch {
            downloadRepository.deleteDownloadedTrack(trackId)
        }
    }

    private fun shareDownload(trackId: Int) {
        // Share logic will be implemented with actual sharing API
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
