package com.elsfm.mobile.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.repository.DownloadRepository
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.PlaylistApi
import com.elsfm.mobile.core.network.api.PlaylistInfo
import com.elsfm.mobile.core.network.api.ProfileApi
import com.elsfm.mobile.core.network.api.RepostApi
import com.elsfm.mobile.feature.library.data.TrackLikeController
import com.elsfm.mobile.feature.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListeningHistoryState(
    val tracks: List<Track> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val likedTrackIds: Set<Int> = emptySet(),
    val likeLoadingTrackIds: Set<Int> = emptySet(),
    val downloadingTrackIds: Set<Int> = emptySet(),
    val downloadedTrackIds: Set<Int> = emptySet(),
    val isPlaylistPickerVisible: Boolean = false,
    val isLoadingPlaylists: Boolean = false,
    val userPlaylists: List<PlaylistInfo> = emptyList(),
    val addToPlaylistTrackId: Int? = null,
    val addToPlaylistLoading: Boolean = false,
) {
    val filteredTracks: List<Track>
        get() = if (searchQuery.isBlank()) {
            tracks
        } else {
            tracks.filter { track ->
                track.name.contains(searchQuery, ignoreCase = true) ||
                    track.artists.any { it.name.contains(searchQuery, ignoreCase = true) }
            }
        }
}

@HiltViewModel
class ListeningHistoryViewModel @Inject constructor(
    private val profileApi: ProfileApi,
    private val trackLikeController: TrackLikeController,
    private val dispatcherProvider: DispatcherProvider,
    private val playerController: PlayerController,
    private val downloadRepository: DownloadRepository,
    private val repostApi: RepostApi,
    private val playlistApi: PlaylistApi,
    private val userDao: UserDao,
) : ViewModel() {
    private val _state = MutableStateFlow(ListeningHistoryState())
    val state: StateFlow<ListeningHistoryState> = _state.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(isLoading = true, error = null) }

            when (val result = profileApi.getRecentlyPlayed()) {
                is ApiResult.Success -> {
                    _state.update { it.copy(tracks = result.data, isLoading = false) }
                }
                is ApiResult.NetworkError,
                is ApiResult.ValidationError,
                is ApiResult.Unauthorized -> {
                    _state.update { it.copy(isLoading = false, error = "Failed to load listening history") }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun playAll() {
        val tracks = _state.value.filteredTracks
        val first = tracks.firstOrNull() ?: return
        playerController.play(first, tracks)
    }

    fun addToQueue(trackId: Int) {
        val track = _state.value.tracks.find { it.id == trackId } ?: return
        playerController.addToQueue(track)
    }

    fun toggleTrackLike(trackId: Int) {
        val currentlyLiked = _state.value.likedTrackIds.contains(trackId)
        _state.update { it.copy(likeLoadingTrackIds = it.likeLoadingTrackIds + trackId) }

        viewModelScope.launch(dispatcherProvider.io) {
            val newLikedState = trackLikeController.toggleLike(trackId, currentlyLiked)
            _state.update {
                it.copy(
                    likedTrackIds = when (newLikedState) {
                        true -> it.likedTrackIds + trackId
                        false -> it.likedTrackIds - trackId
                        null -> it.likedTrackIds
                    },
                    likeLoadingTrackIds = it.likeLoadingTrackIds - trackId,
                    error = if (newLikedState == null) "Failed to update library" else it.error,
                )
            }
        }
    }

    fun downloadTrack(track: Track) {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(downloadingTrackIds = it.downloadingTrackIds + track.id) }
            val result = downloadRepository.downloadTrack(track)
            _state.update {
                it.copy(
                    downloadingTrackIds = it.downloadingTrackIds - track.id,
                    downloadedTrackIds = if (result.isSuccess) it.downloadedTrackIds + track.id else it.downloadedTrackIds,
                    error = if (result.isFailure) "Failed to download track" else it.error,
                )
            }
        }
    }

    fun repostTrack(trackId: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            when (repostApi.toggleTrackRepost(trackId)) {
                is ApiResult.Success -> Unit
                else -> _state.update { it.copy(error = "Failed to repost track") }
            }
        }
    }

    fun showPlaylistPicker(trackId: Int) {
        _state.update { it.copy(isPlaylistPickerVisible = true, addToPlaylistTrackId = trackId) }
        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(isLoadingPlaylists = true) }
            val userId = userDao.get()?.id
            if (userId == null) {
                _state.update { it.copy(isLoadingPlaylists = false, error = "Not signed in") }
                return@launch
            }
            when (val result = playlistApi.getUserPlaylists(userId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isLoadingPlaylists = false, userPlaylists = result.data) }
                }
                else -> _state.update { it.copy(isLoadingPlaylists = false, error = "Failed to load playlists") }
            }
        }
    }

    fun hidePlaylistPicker() {
        _state.update { it.copy(isPlaylistPickerVisible = false) }
    }

    fun addTrackToPlaylist(playlistId: Int) {
        val trackId = _state.value.addToPlaylistTrackId ?: return
        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(addToPlaylistLoading = true) }
            when (playlistApi.addTrackToPlaylist(playlistId, trackId)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(addToPlaylistLoading = false, isPlaylistPickerVisible = false, error = null)
                    }
                }
                else -> _state.update { it.copy(addToPlaylistLoading = false, error = "Failed to add to playlist") }
            }
        }
    }
}
