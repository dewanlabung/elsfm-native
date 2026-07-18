package com.elsfm.mobile.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.dao.LibraryCacheDao
import com.elsfm.mobile.core.database.entity.LibraryCache
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.model.LibrarySections
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.connectivity.NetworkMonitor
import com.elsfm.mobile.core.network.elsfmJson
import com.elsfm.mobile.feature.library.data.LibraryApiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryFilter {
    ALL,
    PLAYLISTS,
    ALBUMS,
    ARTISTS,
    CHANNELS,
}

data class LibraryState(
    val playlists: List<Playlist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val channels: List<Channel> = emptyList(),
    val selectedFilter: LibraryFilter = LibraryFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false,
    val isCreatingPlaylist: Boolean = false,
    val createPlaylistError: String? = null,
    val playlistCreated: Boolean = false,
) {
    val isEmpty: Boolean
        get() = playlists.isEmpty() && albums.isEmpty() && artists.isEmpty() && channels.isEmpty()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryApiRepository,
    private val libraryCacheDao: LibraryCacheDao,
    private val networkMonitor: NetworkMonitor,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            loadCachedIfAvailable()
            performLoad()
        }

        viewModelScope.launch(dispatcherProvider.io) {
            networkMonitor.isOnline
                .drop(1)
                .distinctUntilChanged()
                .filter { it }
                .collect { performLoad() }
        }
    }

    fun loadLibrary() {
        viewModelScope.launch(dispatcherProvider.io) { performLoad() }
    }

    private suspend fun loadCachedIfAvailable() {
        val cached = libraryCacheDao.get() ?: return
        val sections = runCatching {
            elsfmJson().decodeFromString(LibrarySections.serializer(), cached.payloadJson)
        }.getOrNull() ?: return

        _state.value = _state.value.copy(
            playlists = sections.playlists,
            albums = sections.albums,
            artists = sections.artists,
            channels = sections.channels,
            isLoading = false,
        )
    }

    private suspend fun performLoad() {
        val hadContent = !_state.value.isEmpty
        _state.value = _state.value.copy(isLoading = !hadContent, error = null, isOffline = false)

        when (val result = libraryRepository.loadLibrary()) {
            is ApiResult.Success -> {
                _state.value = _state.value.copy(
                    playlists = result.data.playlists,
                    albums = result.data.albums,
                    artists = result.data.artists,
                    channels = result.data.channels,
                    isLoading = false,
                    error = null,
                    isOffline = false,
                )
                cacheLibrary(_state.value)
            }
            is ApiResult.NetworkError -> {
                val hasCached = !_state.value.isEmpty
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = if (hasCached) null else "Failed to load library",
                    isOffline = hasCached,
                )
            }
            is ApiResult.ValidationError, is ApiResult.Unauthorized -> {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = if (_state.value.isEmpty) "Failed to load library" else null,
                )
            }
        }
    }

    private suspend fun cacheLibrary(state: LibraryState) {
        val sections = LibrarySections(
            playlists = state.playlists,
            albums = state.albums,
            artists = state.artists,
            channels = state.channels,
        )
        val json = elsfmJson().encodeToString(LibrarySections.serializer(), sections)
        libraryCacheDao.save(LibraryCache(payloadJson = json))
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
                    cacheLibrary(_state.value)
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

    fun consumePlaylistCreatedEvent() {
        _state.value = _state.value.copy(playlistCreated = false)
    }
}
