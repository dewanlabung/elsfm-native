package com.elsfm.mobile.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.SearchResult
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.SearchApi
import com.elsfm.mobile.feature.library.data.TrackLikeController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Immutable, hoisted UI state for [SearchScreen].
 *
 * Note: [SearchApi] does not currently return albums in its response envelope
 * (the backend search endpoint only returns tracks/artists/playlists), so
 * [albums] will always be empty until that API is extended. The Albums tab
 * is still modeled here so the UI is ready once the backend supports it.
 */
data class SearchUiState(
    val query: String = "",
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null,
    val likedTrackIds: Set<Int> = emptySet(),
    val likeLoadingTrackIds: Set<Int> = emptySet(),
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchApi: SearchApi,
    private val trackLikeController: TrackLikeController,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    fun search(query: String) {
        if (query.isBlank()) {
            _state.value = SearchUiState(query = query)
            return
        }

        _state.value = _state.value.copy(query = query, isLoading = true, error = null)

        viewModelScope.launch {
            when (val result = searchApi.search(query)) {
                is ApiResult.Success -> {
                    val results = result.data
                    _state.value = _state.value.copy(
                        tracks = results.filterIsInstance<SearchResult.TrackResult>().map { it.track },
                        artists = results.filterIsInstance<SearchResult.ArtistResult>().map { it.artist },
                        playlists = results.filterIsInstance<SearchResult.PlaylistResult>().map { it.playlist },
                        isLoading = false,
                        hasSearched = true,
                        error = null,
                    )
                }
                else -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        hasSearched = true,
                        error = "Search failed",
                    )
                }
            }
        }
    }

    fun clearResults() {
        _state.value = SearchUiState()
    }

    fun toggleTrackLike(trackId: Int) {
        val currentlyLiked = _state.value.likedTrackIds.contains(trackId)
        _state.value = _state.value.copy(
            likeLoadingTrackIds = _state.value.likeLoadingTrackIds + trackId,
        )

        viewModelScope.launch {
            val newLikedState = trackLikeController.toggleLike(trackId, currentlyLiked)
            _state.value = _state.value.copy(
                likedTrackIds = when (newLikedState) {
                    true -> _state.value.likedTrackIds + trackId
                    false -> _state.value.likedTrackIds - trackId
                    null -> _state.value.likedTrackIds
                },
                likeLoadingTrackIds = _state.value.likeLoadingTrackIds - trackId,
                error = if (newLikedState == null) "Failed to update library" else _state.value.error,
            )
        }
    }
}
