package com.elsfm.mobile.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.media.PlayHistoryApi
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
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
) : ViewModel() {

    private val _menuState = MutableStateFlow(PlayerMenuState())
    val menuState: StateFlow<PlayerMenuState> = _menuState.asStateFlow()

    val state: StateFlow<PlayerState> = playerController.state

    fun play(track: Track, queue: List<Track>) {
        playerController.play(track, queue)
        viewModelScope.launch { playHistoryApi.recordPlay(track.id) }
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun skipNext() = playerController.skipNext()
    fun skipPrevious() = playerController.skipPrevious()
    fun jumpToQueueItem(track: Track) = playerController.jumpToQueueItem(track)

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
            is PlayerMenuEvent.AddToPlaylist -> {
                viewModelScope.launch {
                    _menuState.value = _menuState.value.copy(addToPlaylistLoading = true)
                    when (menuRepository.addTrackToPlaylist(event.playlistId, event.trackId)) {
                        is ApiResult.Success -> {
                            _menuState.value = _menuState.value.copy(
                                addToPlaylistLoading = false,
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
            is PlayerMenuEvent.ShareTrack -> {
                viewModelScope.launch {
                    _menuState.value = _menuState.value.copy(shareLoading = true)
                    when (menuRepository.shareTrack(event.trackId)) {
                        is ApiResult.Success -> {
                            _menuState.value = _menuState.value.copy(
                                shareLoading = false,
                                error = null
                            )
                        }
                        is ApiResult.NetworkError -> {
                            _menuState.value = _menuState.value.copy(
                                shareLoading = false,
                                error = "Failed to share track"
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
        }
    }
}
