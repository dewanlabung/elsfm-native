package com.elsfm.mobile.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.repository.DownloadRepository
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.TrackListApi
import com.elsfm.mobile.feature.library.data.TrackLikeController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Immutable, hoisted UI state for [PlaylistScreen].
 *
 * `core:network` has no `GET /playlists/{id}` endpoint that returns playlist
 * metadata (name/description/image) — only [TrackListApi.getPlaylistTracks]
 * exists. Until such an endpoint is added, [playlist] is populated from the
 * navigation argument passed into [PlaylistViewModel.loadPlaylist] and only
 * the [tracks] list is backed by a real API call.
 */
data class PlaylistDetailState(
    val playlist: Playlist? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val likedTrackIds: Set<Int> = emptySet(),
    val likeLoadingTrackIds: Set<Int> = emptySet(),
    val downloadingTrackIds: Set<Int> = emptySet(),
    val downloadedTrackIds: Set<Int> = emptySet(),
    val isDownloadingPlaylist: Boolean = false,
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val trackListApi: TrackListApi,
    private val trackLikeController: TrackLikeController,
    private val downloadRepository: DownloadRepository,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(PlaylistDetailState())
    val state: StateFlow<PlaylistDetailState> = _state.asStateFlow()

    fun loadPlaylist(playlist: Playlist) {
        _state.value = _state.value.copy(playlist = playlist, isLoading = true, error = null)

        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = trackListApi.getPlaylistTracks(playlist.id)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(tracks = result.data, isLoading = false)
                }
                is ApiResult.NetworkError,
                is ApiResult.ValidationError,
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load playlist tracks",
                    )
                }
            }
        }
    }

    fun deleteTrack(trackId: Int) {
        _state.value = _state.value.copy(
            tracks = _state.value.tracks.filterNot { it.id == trackId },
        )
    }

    fun toggleTrackLike(trackId: Int) {
        val currentlyLiked = _state.value.likedTrackIds.contains(trackId)
        _state.value = _state.value.copy(
            likeLoadingTrackIds = _state.value.likeLoadingTrackIds + trackId,
        )

        viewModelScope.launch(dispatcherProvider.io) {
            val newLikedState = trackLikeController.toggleLike(trackId, currentlyLiked)
            _state.value = _state.value.copy(
                likedTrackIds = when (newLikedState) {
                    true -> _state.value.likedTrackIds + trackId
                    false -> _state.value.likedTrackIds - trackId
                    null -> _state.value.likedTrackIds
                },
                likeLoadingTrackIds = _state.value.likeLoadingTrackIds - trackId,
                error = if (newLikedState == null) "Failed to update library" else _state.value.error,
            )
        }
    }

    /** "Make available offline" for a single track. */
    fun downloadTrack(track: Track) {
        val playlist = _state.value.playlist
        viewModelScope.launch(dispatcherProvider.io) {
            _state.value = _state.value.copy(
                downloadingTrackIds = _state.value.downloadingTrackIds + track.id,
            )
            val result = downloadRepository.downloadTrack(
                track,
                playlistId = playlist?.id,
                playlistName = playlist?.name,
            )
            _state.value = _state.value.copy(
                downloadingTrackIds = _state.value.downloadingTrackIds - track.id,
                downloadedTrackIds = if (result.isSuccess) {
                    _state.value.downloadedTrackIds + track.id
                } else {
                    _state.value.downloadedTrackIds
                },
                error = if (result.isFailure) "Failed to download track" else _state.value.error,
            )
        }
    }

    /**
     * "Make available offline" for the whole playlist - same per-track download
     * looped client-side as [downloadTrack], matching the real PWA's own
     * "Downloading N tracks..." behavior (there is no bulk backend endpoint).
     */
    fun downloadPlaylist() {
        val tracks = _state.value.tracks
        if (tracks.isEmpty()) return
        val playlist = _state.value.playlist

        viewModelScope.launch(dispatcherProvider.io) {
            _state.value = _state.value.copy(isDownloadingPlaylist = true)
            for (track in tracks) {
                _state.value = _state.value.copy(
                    downloadingTrackIds = _state.value.downloadingTrackIds + track.id,
                )
                val result = downloadRepository.downloadTrack(
                    track,
                    playlistId = playlist?.id,
                    playlistName = playlist?.name,
                )
                _state.value = _state.value.copy(
                    downloadingTrackIds = _state.value.downloadingTrackIds - track.id,
                    downloadedTrackIds = if (result.isSuccess) {
                        _state.value.downloadedTrackIds + track.id
                    } else {
                        _state.value.downloadedTrackIds
                    },
                )
            }
            _state.value = _state.value.copy(isDownloadingPlaylist = false)
        }
    }
}
