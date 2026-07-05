package com.elsfm.mobile.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ChannelApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryState(
    val channels: List<Channel> = emptyList(),
    val selectedChannelId: Int? = null,
    val playlistsInChannel: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val channelApi: ChannelApi,
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init {
        loadChannels()
    }

    fun loadChannels() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = channelApi.getChannels()
            if (result is ApiResult.Success) {
                _state.value = _state.value.copy(channels = result.data, isLoading = false)
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load channels"
                )
            }
        }
    }

    fun selectChannel(channelId: Int) {
        _state.value = _state.value.copy(selectedChannelId = channelId)
    }
}
