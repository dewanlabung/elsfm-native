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
            is PlayerMenuEvent.AddToPlaylist -> {
                addTrackToPlaylist(event.trackId, event.playlistId)
            }
            is PlayerMenuEvent.ShareTrack -> {
                shareTrack(event.trackId)
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
