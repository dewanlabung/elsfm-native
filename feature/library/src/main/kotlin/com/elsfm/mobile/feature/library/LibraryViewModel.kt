package com.elsfm.mobile.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Artist
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
    ARTISTS,
    CHANNELS,
}

/**
 * Immutable, hoisted UI state for [LibraryScreen].
 *
 * Playlists, albums and artists are all backed by real, per-user API data via
 * [LibraryApiRepository]; channels are the app's home/discovery channel list.
 */
data class LibraryState(
    val playlists: List<Playlist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val channels: List<Channel> = emptyList(),
    val selectedFilter: LibraryFilter = LibraryFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCreatingPlaylist: Boolean = false,
    val createPlaylistError: String? = null,
    /** One-shot signal the screen consumes to dismiss the create-playlist dialog. */
    val playlistCreated: Boolean = false,
) {
    val isEmpty: Boolean
        get() = playlists.isEmpty() && albums.isEmpty() && artists.isEmpty() && channels.isEmpty()
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
                    artists = result.data.artists,
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

    fun createPlaylist(name: String) {
        _state.value = _state.value.copy(isCreatingPlaylist = true, createPlaylistError = null)

        viewModelScope.launch {
            when (val result = libraryRepository.createPlaylist(name)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        playlists = _state.value.playlists + result.data,
                        isCreatingPlaylist = false,
                        playlistCreated = true,
                    )
                }
                is ApiResult.ValidationError -> {
                    val errorMessages = result.fields.values.flatten().joinToString(", ")
                    _state.value = _state.value.copy(
                        isCreatingPlaylist = false,
                        createPlaylistError = errorMessages.ifEmpty { "Could not create playlist" },
                    )
                }
                is ApiResult.NetworkError, is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(
                        isCreatingPlaylist = false,
                        createPlaylistError = "Could not create playlist",
                    )
                }
            }
        }
    }

    /** Called by the screen once it has reacted to [LibraryState.playlistCreated]. */
    fun consumePlaylistCreatedEvent() {
        _state.value = _state.value.copy(playlistCreated = false)
    }
}
