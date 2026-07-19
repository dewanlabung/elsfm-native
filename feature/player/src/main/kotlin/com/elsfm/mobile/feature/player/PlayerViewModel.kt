package com.elsfm.mobile.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.repository.DownloadRepository
import com.elsfm.mobile.core.media.PlayHistoryApi
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.UserApi
import com.elsfm.mobile.feature.player.data.PlayerMenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val playHistoryApi: PlayHistoryApi,
    private val menuRepository: PlayerMenuRepository,
    private val userApi: UserApi,
    private val userDao: UserDao,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    private val _menuState = MutableStateFlow(PlayerMenuState())
    val menuState: StateFlow<PlayerMenuState> = _menuState.asStateFlow()

    val state: StateFlow<PlayerState> = playerController.state

    fun play(track: Track, queue: List<Track>) {
        // A new track starts unliked until the user toggles it this session -
        // there is no "is this track already liked" lookup endpoint, matching
        // the same simplification used by Album/Playlist track rows.
        _menuState.value = _menuState.value.copy(isLiked = false, isLikeLoading = false)
        playerController.play(track, queue)
        playHistoryApi.startNewQueueSession()
        viewModelScope.launch { playHistoryApi.recordPlay(track.id) }
        viewModelScope.launch {
            if (downloadRepository.isDownloaded(track.id)) {
                _menuState.value = _menuState.value.copy(
                    downloadedTrackIds = _menuState.value.downloadedTrackIds + track.id,
                )
            }
        }
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun skipNext() = playerController.skipNext()
    fun skipPrevious() = playerController.skipPrevious()
    fun jumpToQueueItem(track: Track) = playerController.jumpToQueueItem(track)
    fun toggleShuffle() = playerController.toggleShuffle()
    fun cycleRepeatMode() = playerController.cycleRepeatMode()
    fun startSleepTimer(minutes: Int) = playerController.startSleepTimer(minutes)
    fun cancelSleepTimer() = playerController.cancelSleepTimer()
    fun setPlaybackSpeed(speed: Float) = playerController.setPlaybackSpeed(speed)
    fun setVolume(volume: Float) = playerController.setVolume(volume)

    fun addToQueue(track: Track) = playerController.addToQueue(track)

    fun toggleLike() {
        val track = state.value.currentTrack ?: return
        val currentlyLiked = menuState.value.isLiked
        _menuState.value = _menuState.value.copy(isLikeLoading = true)

        viewModelScope.launch {
            val result = if (currentlyLiked) {
                userApi.removeTrackFromLibrary(track.id)
            } else {
                userApi.addTrackToLibrary(track.id)
            }
            when (result) {
                is ApiResult.Success -> {
                    _menuState.value = _menuState.value.copy(
                        isLiked = result.data,
                        isLikeLoading = false,
                    )
                }
                is ApiResult.NetworkError,
                is ApiResult.ValidationError,
                is ApiResult.Unauthorized -> {
                    _menuState.value = _menuState.value.copy(
                        isLikeLoading = false,
                        error = "Failed to update library",
                    )
                }
            }
        }
    }

    fun onMenuEvent(event: PlayerMenuEvent) {
        when (event) {
            is PlayerMenuEvent.ShowMenu -> {
                _menuState.value = _menuState.value.copy(
                    isMenuVisible = true,
                    selectedTrackId = event.trackId
                )
            }
            PlayerMenuEvent.HideMenu -> {
                _menuState.value = _menuState.value.copy(isMenuVisible = false)
            }
            is PlayerMenuEvent.PlayNext -> {
                state.value.currentTrack?.takeIf { it.id == event.trackId }?.let { track ->
                    playerController.playNext(track)
                }
                _menuState.value = _menuState.value.copy(isMenuVisible = false)
            }
            is PlayerMenuEvent.AddToQueue -> {
                // Purely local: no backend call, just appends to the in-memory queue.
                state.value.currentTrack?.takeIf { it.id == event.trackId }?.let { track ->
                    playerController.addToQueue(track)
                }
                _menuState.value = _menuState.value.copy(isMenuVisible = false)
            }
            is PlayerMenuEvent.AddToLibrary -> {
                viewModelScope.launch {
                    _menuState.value = _menuState.value.copy(addToLibraryLoading = true)
                    when (menuRepository.addTrackToLibrary(event.trackId)) {
                        is ApiResult.Success -> {
                            _menuState.value = _menuState.value.copy(
                                addToLibraryLoading = false,
                                error = null
                            )
                        }
                        is ApiResult.NetworkError -> {
                            _menuState.value = _menuState.value.copy(
                                addToLibraryLoading = false,
                                error = "Failed to add track to library"
                            )
                        }
                        else -> {}
                    }
                }
            }
            is PlayerMenuEvent.ShowPlaylistPicker -> {
                _menuState.value = _menuState.value.copy(
                    isMenuVisible = false,
                    isPlaylistPickerVisible = true,
                    selectedTrackId = event.trackId,
                )
                viewModelScope.launch {
                    _menuState.value = _menuState.value.copy(isLoadingPlaylists = true)
                    val userId = userDao.get()?.id
                    if (userId == null) {
                        _menuState.value = _menuState.value.copy(
                            isLoadingPlaylists = false,
                            error = "Not signed in",
                        )
                        return@launch
                    }
                    when (val result = menuRepository.getUserPlaylists(userId)) {
                        is ApiResult.Success -> {
                            _menuState.value = _menuState.value.copy(
                                isLoadingPlaylists = false,
                                userPlaylists = result.data,
                            )
                        }
                        else -> {
                            _menuState.value = _menuState.value.copy(
                                isLoadingPlaylists = false,
                                error = "Failed to load playlists",
                            )
                        }
                    }
                }
            }
            PlayerMenuEvent.HidePlaylistPicker -> {
                _menuState.value = _menuState.value.copy(isPlaylistPickerVisible = false)
            }
            is PlayerMenuEvent.AddToPlaylist -> {
                viewModelScope.launch {
                    _menuState.value = _menuState.value.copy(addToPlaylistLoading = true)
                    when (menuRepository.addTrackToPlaylist(event.playlistId, event.trackId)) {
                        is ApiResult.Success -> {
                            _menuState.value = _menuState.value.copy(
                                addToPlaylistLoading = false,
                                isPlaylistPickerVisible = false,
                                error = null
                            )
                        }
                        is ApiResult.NetworkError -> {
                            _menuState.value = _menuState.value.copy(
                                addToPlaylistLoading = false,
                                error = "Failed to add track to playlist"
                            )
                        }
                        else -> {}
                    }
                }
            }
            is PlayerMenuEvent.Repost -> {
                viewModelScope.launch {
                    _menuState.value = _menuState.value.copy(repostLoading = true)
                    when (menuRepository.repostTrack(event.trackId)) {
                        is ApiResult.Success -> {
                            _menuState.value = _menuState.value.copy(
                                repostLoading = false,
                                error = null
                            )
                        }
                        is ApiResult.NetworkError -> {
                            _menuState.value = _menuState.value.copy(
                                repostLoading = false,
                                error = "Failed to repost track"
                            )
                        }
                        else -> {}
                    }
                }
            }
            is PlayerMenuEvent.MakeAvailableOffline -> {
                val track = state.value.currentTrack?.takeIf { it.id == event.trackId }
                if (track != null) {
                    viewModelScope.launch {
                        _menuState.value = _menuState.value.copy(
                            downloadingTrackIds = _menuState.value.downloadingTrackIds + track.id,
                        )
                        val result = downloadRepository.downloadTrack(
                            track,
                            albumId = track.album?.id,
                            albumName = track.album?.name,
                        )
                        result.onFailure {
                            _menuState.value = _menuState.value.copy(error = "Failed to download track")
                        }
                        _menuState.value = _menuState.value.copy(
                            downloadingTrackIds = _menuState.value.downloadingTrackIds - track.id,
                            downloadedTrackIds = if (result.isSuccess) {
                                _menuState.value.downloadedTrackIds + track.id
                            } else {
                                _menuState.value.downloadedTrackIds
                            },
                        )
                    }
                }
            }
        }
    }
}
