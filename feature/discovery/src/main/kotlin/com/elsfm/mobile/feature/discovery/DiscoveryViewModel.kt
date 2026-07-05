package com.elsfm.mobile.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ChannelApi
import com.elsfm.mobile.core.network.api.TrackListApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val POPULAR_SONGS_PLAYLIST_ID = 8

data class DiscoveryState(
    val featuredChannels: List<Channel> = emptyList(),
    val popularTracks: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val channelApi: ChannelApi,
    private val trackListApi: TrackListApi,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(DiscoveryState())
    val state: StateFlow<DiscoveryState> = _state.asStateFlow()

    init {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.value = _state.value.copy(isLoading = true, error = null)
            loadChannels()
            loadPopularTracks()
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    private suspend fun loadChannels() {
        when (val result = channelApi.getChannels()) {
            is ApiResult.Success -> {
                _state.value = _state.value.copy(featuredChannels = result.data)
            }
            is ApiResult.NetworkError,
            is ApiResult.ValidationError,
            is ApiResult.Unauthorized -> {
                _state.value = _state.value.copy(error = "Failed to load featured playlists")
            }
        }
    }

    private suspend fun loadPopularTracks() {
        when (val result = trackListApi.getPlaylistTracks(POPULAR_SONGS_PLAYLIST_ID)) {
            is ApiResult.Success -> {
                _state.value = _state.value.copy(popularTracks = result.data)
            }
            is ApiResult.NetworkError,
            is ApiResult.ValidationError,
            is ApiResult.Unauthorized -> {
                _state.value = _state.value.copy(error = "Failed to load popular songs")
            }
        }
    }
}
