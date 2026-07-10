package com.elsfm.mobile.feature.discovery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ChannelApi
import com.elsfm.mobile.core.network.api.ChannelContentResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal const val CHANNEL_DETAIL_CHANNEL_ID_ARG = "channelId"

// Optional: the channel's name, as already known by the caller (e.g. from
// DiscoveryUiState.exploreMoreChannelTitle) so the detail screen can show a
// real title immediately, before its own content fetch resolves. Not part of
// ChannelApi.getChannelContent's response.
internal const val CHANNEL_DETAIL_TITLE_ARG = "channelTitle"

/**
 * Immutable, hoisted UI state for [ChannelDetailScreen].
 *
 * A channel's own content can be tracks, playlists, albums, or further
 * nested channels depending on the backend's `config.contentModel` for that
 * channel (see [ChannelApi.getChannelContent]); [content] renders whichever
 * one comes back.
 */
data class ChannelDetailUiState(
    val title: String? = null,
    val content: ChannelContentResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ChannelDetailViewModel @Inject constructor(
    private val channelApi: ChannelApi,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val channelId: Int = checkNotNull(savedStateHandle[CHANNEL_DETAIL_CHANNEL_ID_ARG]) {
        "ChannelDetailViewModel requires a '$CHANNEL_DETAIL_CHANNEL_ID_ARG' argument"
    }

    private val _state = MutableStateFlow(
        ChannelDetailUiState(title = savedStateHandle[CHANNEL_DETAIL_TITLE_ARG]),
    )
    val state: StateFlow<ChannelDetailUiState> = _state.asStateFlow()

    init {
        loadChannel()
    }

    fun loadChannel() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            when (val result = channelApi.getChannelContent(channelId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        content = result.data,
                        isLoading = false,
                    )
                }
                is ApiResult.NetworkError,
                is ApiResult.ValidationError,
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load channel",
                    )
                }
            }
        }
    }
}
