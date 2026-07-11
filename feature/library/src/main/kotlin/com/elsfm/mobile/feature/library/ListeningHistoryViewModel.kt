package com.elsfm.mobile.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ProfileApi
import com.elsfm.mobile.feature.library.data.TrackLikeController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Immutable, hoisted UI state for [ListeningHistoryScreen].
 *
 * Backed by the real `GET api/v1/tracks/plays/me` endpoint
 * ([ProfileApi.getRecentlyPlayed]), the same call already used by Discovery
 * and Profile for "recently played" data.
 */
data class ListeningHistoryState(
    val tracks: List<Track> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val likedTrackIds: Set<Int> = emptySet(),
    val likeLoadingTrackIds: Set<Int> = emptySet(),
) {
    val filteredTracks: List<Track>
        get() = if (searchQuery.isBlank()) {
            tracks
        } else {
            tracks.filter { track ->
                track.name.contains(searchQuery, ignoreCase = true) ||
                    track.artists.any { it.name.contains(searchQuery, ignoreCase = true) }
            }
        }
}

@HiltViewModel
class ListeningHistoryViewModel @Inject constructor(
    private val profileApi: ProfileApi,
    private val trackLikeController: TrackLikeController,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(ListeningHistoryState())
    val state: StateFlow<ListeningHistoryState> = _state.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            when (val result = profileApi.getRecentlyPlayed()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(tracks = result.data, isLoading = false)
                }
                is ApiResult.NetworkError,
                is ApiResult.ValidationError,
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load listening history",
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun playAll() {
        // Playback wiring happens in a follow-up (see PlaylistViewModel.playAll /
        // AlbumViewModel.playAll for the same established placeholder pattern).
        // No-op retained so the "Play" button has a stable action to hoist and
        // callers can observe intent via an onPlayAll callback.
    }

    fun toggleTrackLike(trackId: Int) {
        val currentlyLiked = _state.value.likedTrackIds.contains(trackId)
        _state.value = _state.value.copy(
            likeLoadingTrackIds = _state.value.likeLoadingTrackIds + trackId,
        )

        viewModelScope.launch(dispatcherProvider.io) {
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
