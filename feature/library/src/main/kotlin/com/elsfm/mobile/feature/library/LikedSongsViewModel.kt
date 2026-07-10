package com.elsfm.mobile.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.UserApi
import com.elsfm.mobile.feature.library.data.TrackLikeController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Immutable, hoisted UI state for [LikedSongsScreen].
 *
 * Backed by the real `GET api/v1/users/{user}/liked-tracks` endpoint
 * ([UserApi.getLikedTracks]) — there is no boolean `is_liked` field on [Track],
 * so this screen's list itself doubles as the "liked" source of truth: every
 * track present in [tracks] is, by construction, currently liked.
 */
data class LikedSongsState(
    val tracks: List<Track> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
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
class LikedSongsViewModel @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao,
    private val trackLikeController: TrackLikeController,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(LikedSongsState())
    val state: StateFlow<LikedSongsState> = _state.asStateFlow()

    init {
        loadLikedSongs()
    }

    fun loadLikedSongs() {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val userId = userDao.get()?.id
            if (userId == null) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "You must be signed in to view liked songs",
                )
                return@launch
            }

            when (val result = userApi.getLikedTracks(userId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(tracks = result.data, isLoading = false)
                }
                is ApiResult.NetworkError,
                is ApiResult.ValidationError,
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load liked songs",
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

    /**
     * Unliking a track on this screen removes it from the list immediately,
     * since every track shown here is liked by definition.
     */
    fun toggleTrackLike(trackId: Int) {
        _state.value = _state.value.copy(
            likeLoadingTrackIds = _state.value.likeLoadingTrackIds + trackId,
        )

        viewModelScope.launch(dispatcherProvider.io) {
            val newLikedState = trackLikeController.toggleLike(trackId, currentlyLiked = true)
            _state.value = _state.value.copy(
                tracks = if (newLikedState == false) {
                    _state.value.tracks.filterNot { it.id == trackId }
                } else {
                    _state.value.tracks
                },
                likeLoadingTrackIds = _state.value.likeLoadingTrackIds - trackId,
                error = if (newLikedState == null) "Failed to update library" else _state.value.error,
            )
        }
    }
}
