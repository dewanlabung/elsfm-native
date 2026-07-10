package com.elsfm.mobile.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.feature.library.data.LibraryApiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryFilter {
    ALL,
    PLAYLISTS,
    ALBUMS,
    CHANNELS,
}

/**
 * Immutable, hoisted UI state for [LibraryScreen].
 *
 * Playlists, albums and channels are all backed by real API data via
 * [LibraryApiRepository].
 */
data class LibraryState(
    val playlists: List<Playlist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val channels: List<Channel> = emptyList(),
    val selectedFilter: LibraryFilter = LibraryFilter.ALL,
    val selectedChannelId: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val isEmpty: Boolean
        get() = playlists.isEmpty() && albums.isEmpty() && channels.isEmpty()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryApiRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init {
        loadLibrary()
    }

    fun loadLibrary() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = libraryRepository.loadLibrary()
            if (result is ApiResult.Success) {
                _state.value = _state.value.copy(
                    playlists = result.data.playlists,
                    albums = result.data.albums,
                    channels = result.data.channels,
                    isLoading = false,
                )
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load library",
                )
            }
        }
    }

    fun selectFilter(filter: LibraryFilter) {
        _state.value = _state.value.copy(selectedFilter = filter)
    }

    fun selectChannel(channelId: Int) {
        _state.value = _state.value.copy(selectedChannelId = channelId)
    }
}
