package com.elsfm.mobile.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.TrackListApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Immutable, hoisted UI state for [AlbumScreen].
 *
 * `core:network` has no `GET /albums/{id}` or `GET /albums/{id}/tracks`
 * endpoint. Album metadata comes from the navigation argument passed into
 * [AlbumViewModel.loadAlbum]; the same [TrackListApi.getPlaylistTracks] call
 * used for playlists is reused here since the backend models an album's
 * tracks the same way a playlist's tracks are exposed (both are just track
 * collections keyed by an id in this API).
 */
data class AlbumDetailState(
    val album: Album? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val trackListApi: TrackListApi,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(AlbumDetailState())
    val state: StateFlow<AlbumDetailState> = _state.asStateFlow()

    fun loadAlbum(album: Album) {
        _state.value = _state.value.copy(album = album, isLoading = true, error = null)

        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = trackListApi.getPlaylistTracks(album.id)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(tracks = result.data, isLoading = false)
                }
                is ApiResult.NetworkError,
                is ApiResult.ValidationError,
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load album tracks",
                    )
                }
            }
        }
    }

    fun playAll() {
        // Playback wiring happens in Task 9 (navigation/player integration).
        // No-op placeholder retained so the "Play All" button has a stable
        // action to hoist and callers can observe intent via onPlayAll callback.
    }
}
