package com.elsfm.mobile.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ChannelApi
import com.elsfm.mobile.core.network.api.ChannelContentResult
import com.elsfm.mobile.core.network.api.ProfileApi
import com.elsfm.mobile.core.network.api.TrackListApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
 * All sections are backed by real backend data: [featured] and
 * [newReleases] come from Channel 5's nested sub-channels — fetched via
 * [ChannelApi.getChannels] and then [ChannelApi.getChannelContent] for each
 * sub-channel — while [popular] and [recentlyPlayed] are backed by the
 * existing [TrackListApi] and [ProfileApi] endpoints used elsewhere in the
 * app.
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
    private val channelApi: ChannelApi,
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
                val homeSectionsDeferred = async { loadHomeSections() }

                val popular = popularDeferred.await()
                val recentlyPlayed = recentlyPlayedDeferred.await()
                val homeSections = homeSectionsDeferred.await()

                if (popular == null) {
                    loadError = "Failed to load popular songs"
                }
                if (recentlyPlayed == null) {
                    loadError = loadError ?: "Failed to load recently played"
                }
                if (homeSections.featured == null || homeSections.newReleases == null) {
                    loadError = loadError ?: "Failed to load home sections"
                }

                _state.value = _state.value.copy(
                    featured = homeSections.featured.orEmpty(),
                    popular = popular.orEmpty(),
                    newReleases = homeSections.newReleases.orEmpty(),
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

    /**
     * Fetches Channel 5's real nested sub-channels, then loads each
     * sub-channel's own content in parallel to find real playlists (for
     * "Featured") and real albums (for "New Releases"). The two halves are
     * independently nullable so a failure fetching one content model does
     * not blank out the other.
     */
    private suspend fun loadHomeSections(): HomeSections {
        val subChannels = when (val result = channelApi.getChannels()) {
            is ApiResult.Success -> result.data
            is ApiResult.NetworkError,
            is ApiResult.ValidationError,
            is ApiResult.Unauthorized -> return HomeSections(featured = null, newReleases = null)
        }

        return coroutineScope {
            val contentResults = subChannels
                .map { channel -> async { channelApi.getChannelContent(channel.id) } }
                .awaitAll()

            HomeSections(
                featured = contentResults.firstNotNullOfOrNull { it.asPlaylists() } ?: emptyList(),
                newReleases = contentResults.firstNotNullOfOrNull { it.asAlbums() } ?: emptyList(),
            )
        }
    }

    private fun ApiResult<ChannelContentResult>.asPlaylists(): List<Playlist>? {
        return ((this as? ApiResult.Success)?.data as? ChannelContentResult.Playlists)?.items
    }

    private fun ApiResult<ChannelContentResult>.asAlbums(): List<Album>? {
        return ((this as? ApiResult.Success)?.data as? ChannelContentResult.Albums)?.items
    }

    private data class HomeSections(
        val featured: List<Playlist>?,
        val newReleases: List<Album>?,
    )
}
