package com.elsfm.mobile.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.AlbumApi
import com.elsfm.mobile.feature.library.data.TrackLikeController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal const val ALBUM_ID_ARG = "albumId"

/**
 * Immutable, hoisted UI state for [AlbumScreen]. Both [album] and [tracks]
 * come from the single real `GET /albums/{id}` call ([AlbumApi.getAlbum]) —
 * the backend has no separate tracks endpoint, it nests tracks in the album
 * response.
 */
data class AlbumDetailState(
    val album: Album? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val likedTrackIds: Set<Int> = emptySet(),
    val likeLoadingTrackIds: Set<Int> = emptySet(),
)

@HiltViewModel
class AlbumViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val albumApi: AlbumApi,
    private val trackLikeController: TrackLikeController,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val albumId: Int = checkNotNull(savedStateHandle[ALBUM_ID_ARG]) {
        "AlbumViewModel requires an albumId argument"
    }

    private val _state = MutableStateFlow(AlbumDetailState())
    val state: StateFlow<AlbumDetailState> = _state.asStateFlow()

    init {
        loadAlbum()
    }

    private fun loadAlbum() {
        _state.value = _state.value.copy(isLoading = true, error = null)

        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = albumApi.getAlbum(albumId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        album = result.data,
                        tracks = result.data.tracks,
                        isLoading = false,
                    )
                }
                is ApiResult.NetworkError,
                is ApiResult.ValidationError,
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load album",
                    )
                }
            }
        }
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
