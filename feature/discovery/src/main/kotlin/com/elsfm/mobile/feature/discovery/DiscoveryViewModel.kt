package com.elsfm.mobile.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ProfileApi
import com.elsfm.mobile.core.network.api.TrackListApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val POPULAR_SONGS_PLAYLIST_ID = 8

/**
 * Immutable, hoisted UI state for [DiscoveryScreen].
 *
 * Featured playlists and new-release albums are not yet backed by a real
 * backend endpoint (no `List<Playlist>` or `List<Album>` API exists in
 * `core:network` at this time), so [DiscoveryViewModel] populates them with
 * static sample data. Popular songs and recently played tracks are backed by
 * the real [TrackListApi] and [ProfileApi] endpoints already used elsewhere
 * in the app.
 */
data class DiscoveryUiState(
    val featured: List<Playlist> = emptyList(),
    val popular: List<Track> = emptyList(),
    val newReleases: List<Album> = emptyList(),
    val recentlyPlayed: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val trackListApi: TrackListApi,
    private val profileApi: ProfileApi,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(DiscoveryUiState())
    val state: StateFlow<DiscoveryUiState> = _state.asStateFlow()

    init {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            var loadError: String? = null

            coroutineScope {
                val popularDeferred = async { loadPopularTracks() }
                val recentlyPlayedDeferred = async { loadRecentlyPlayed() }

                val popular = popularDeferred.await()
                val recentlyPlayed = recentlyPlayedDeferred.await()

                if (popular == null) {
                    loadError = "Failed to load popular songs"
                }
                if (recentlyPlayed == null) {
                    loadError = loadError ?: "Failed to load recently played"
                }

                _state.value = _state.value.copy(
                    featured = SampleDiscoveryData.featuredPlaylists,
                    popular = popular.orEmpty(),
                    newReleases = SampleDiscoveryData.newReleaseAlbums,
                    recentlyPlayed = recentlyPlayed.orEmpty(),
                )
            }

            _state.value = _state.value.copy(isLoading = false, error = loadError)
        }
    }

    private suspend fun loadPopularTracks(): List<Track>? {
        return when (val result = trackListApi.getPlaylistTracks(POPULAR_SONGS_PLAYLIST_ID)) {
            is ApiResult.Success -> result.data
            is ApiResult.NetworkError,
            is ApiResult.ValidationError,
            is ApiResult.Unauthorized -> null
        }
    }

    private suspend fun loadRecentlyPlayed(): List<Track>? {
        return when (val result = profileApi.getRecentlyPlayed()) {
            is ApiResult.Success -> result.data
            is ApiResult.NetworkError,
            is ApiResult.ValidationError,
            is ApiResult.Unauthorized -> null
        }
    }
}
