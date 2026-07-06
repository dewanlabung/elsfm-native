package com.elsfm.mobile.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.dao.DownloadedTrackDao
import com.elsfm.mobile.core.database.entity.DownloadedTrack
import com.elsfm.mobile.core.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val downloadedTrackDao: DownloadedTrackDao,
    private val downloadManager: DownloadManager,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(DownloadsState())
    val state: StateFlow<DownloadsState> = _state.asStateFlow()

    init {
        loadDownloads()
    }

    fun loadDownloads() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            downloadedTrackDao.getAll().collect { tracks ->
                val uiTracks = tracks.map { track ->
                    DownloadedTrackUI(
                        trackId = track.trackId,
                        title = "Track ${track.trackId}",
                        artist = "Unknown",
                        artworkUrl = null,
                        fileSize = formatFileSize(track.fileSizeBytes),
                        downloadedAt = track.downloadedAt,
                        isOffline = true
                    )
                }
                _state.update { it.copy(downloadedTracks = uiTracks, isLoading = false) }
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
            bytes >= 1_000_000 -> "%.2f MB".format(bytes / 1_000_000.0)
            bytes >= 1_000 -> "%.2f KB".format(bytes / 1_000.0)
            else -> "$bytes B"
        }
    }

    fun deleteDownload(trackId: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            downloadManager.deleteDownload(trackId)
            downloadedTrackDao.delete(trackId)
        }
    }

    fun downloadTrack(track: Track) {
        viewModelScope.launch(dispatcherProvider.io) {
            downloadManager.downloadTrack(track) { progress ->
                _state.update {
                    it.copy(downloadProgress = it.downloadProgress + (track.id to progress))
                }
            }.onSuccess { file ->
                val downloaded = DownloadedTrack(
                    trackId = track.id,
                    fileName = file.name,
                    fileSizeBytes = file.length(),
                )
                downloadedTrackDao.insert(downloaded)
                _state.update { it.copy(downloadProgress = it.downloadProgress - track.id) }
            }.onFailure { error ->
                _state.update { it.copy(error = error.message) }
            }
        }
    }
}
