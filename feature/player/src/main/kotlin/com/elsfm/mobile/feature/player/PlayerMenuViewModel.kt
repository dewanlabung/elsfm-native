package com.elsfm.mobile.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.feature.player.data.PlayerMenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerMenuViewModel @Inject constructor(
    private val menuRepository: PlayerMenuRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerMenuState())
    val state: StateFlow<PlayerMenuState> = _state.asStateFlow()

    fun onEvent(event: PlayerMenuEvent) {
        when (event) {
            is PlayerMenuEvent.ShowMenu -> {
                _state.value = _state.value.copy(
                    isMenuVisible = true,
                    selectedTrackId = event.trackId
                )
            }
            PlayerMenuEvent.HideMenu -> {
                _state.value = _state.value.copy(isMenuVisible = false)
            }
            is PlayerMenuEvent.AddToQueue -> {
                // Not actionable here: this ViewModel has no PlayerController dependency and
                // is currently unused by PlayerScreen (see PlayerViewModel.onMenuEvent, which
                // is the live wiring and owns queue mutation via PlayerController.addToQueue).
            }
            is PlayerMenuEvent.AddToLibrary -> {
                addTrackToLibrary(event.trackId)
            }
            is PlayerMenuEvent.AddToPlaylist -> {
                addTrackToPlaylist(event.trackId, event.playlistId)
            }
            is PlayerMenuEvent.ShareTrack -> {
                shareTrack(event.trackId)
            }
            is PlayerMenuEvent.Repost -> {
                repostTrack(event.trackId)
            }
        }
    }

    private fun addTrackToLibrary(trackId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(addToLibraryLoading = true)
            when (menuRepository.addTrackToLibrary(trackId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(addToLibraryLoading = false, error = null)
                }
                is ApiResult.NetworkError -> {
                    _state.value = _state.value.copy(
                        addToLibraryLoading = false,
                        error = "Failed to add track to library"
                    )
                }
                is ApiResult.ValidationError -> {
                    _state.value = _state.value.copy(addToLibraryLoading = false, error = "Invalid request")
                }
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(addToLibraryLoading = false, error = "Authentication required")
                }
            }
        }
    }

    private fun repostTrack(trackId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(repostLoading = true)
            when (menuRepository.repostTrack(trackId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(repostLoading = false, error = null)
                }
                is ApiResult.NetworkError -> {
                    _state.value = _state.value.copy(
                        repostLoading = false,
                        error = "Failed to repost track"
                    )
                }
                is ApiResult.ValidationError -> {
                    _state.value = _state.value.copy(repostLoading = false, error = "Invalid request")
                }
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(repostLoading = false, error = "Authentication required")
                }
            }
        }
    }

    private fun addTrackToPlaylist(trackId: Int, playlistId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(addToPlaylistLoading = true)
            when (val result = menuRepository.addTrackToPlaylist(playlistId, trackId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        addToPlaylistLoading = false,
                        error = null,
                        isMenuVisible = false
                    )
                }
                is ApiResult.NetworkError -> {
                    _state.value = _state.value.copy(
                        addToPlaylistLoading = false,
                        error = "Failed to add track to playlist"
                    )
                }
                is ApiResult.ValidationError -> {
                    _state.value = _state.value.copy(
                        addToPlaylistLoading = false,
                        error = "Invalid request"
                    )
                }
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(
                        addToPlaylistLoading = false,
                        error = "Authentication required"
                    )
                }
            }
        }
    }

    private fun shareTrack(trackId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(shareLoading = true)
            when (val result = menuRepository.shareTrack(trackId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        shareLoading = false,
                        error = null,
                        isMenuVisible = false
                    )
                }
                is ApiResult.NetworkError -> {
                    _state.value = _state.value.copy(
                        shareLoading = false,
                        error = "Failed to share track"
                    )
                }
                is ApiResult.ValidationError -> {
                    _state.value = _state.value.copy(
                        shareLoading = false,
                        error = "Invalid request"
                    )
                }
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(
                        shareLoading = false,
                        error = "Authentication required"
                    )
                }
            }
        }
    }
}
