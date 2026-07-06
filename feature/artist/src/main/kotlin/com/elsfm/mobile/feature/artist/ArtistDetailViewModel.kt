package com.elsfm.mobile.feature.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ArtistApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val artistApi: ArtistApi,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val dispatcher = dispatcherProvider.io

    private val _state = MutableStateFlow(ArtistDetailState())
    val state: StateFlow<ArtistDetailState> = _state.asStateFlow()

    init {
        savedStateHandle.get<Int>("artistId")?.let { artistId ->
            loadArtistDetails(artistId)
        }
    }

    private fun loadArtistDetails(id: Int) {
        viewModelScope.launch(dispatcher) {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                coroutineScope {
                    val artistResult = artistApi.getArtist(id)
                    val tracksResult = artistApi.getArtistTracks(id)

                    when {
                        artistResult is ApiResult.Success && tracksResult is ApiResult.Success -> {
                            _state.value = _state.value.copy(
                                artist = artistResult.data,
                                tracks = tracksResult.data,
                                albums = sampleArtistAlbums(),
                                isLoading = false
                            )
                        }
                        artistResult is ApiResult.Success -> {
                            _state.value = _state.value.copy(
                                artist = artistResult.data,
                                tracks = emptyList(),
                                albums = sampleArtistAlbums(),
                                isLoading = false
                            )
                        }
                        else -> {
                            val errorMsg = when (artistResult) {
                                is ApiResult.NetworkError -> artistResult.cause.message ?: "Network error"
                                is ApiResult.ValidationError -> "Validation error"
                                is ApiResult.Unauthorized -> "Unauthorized"
                                else -> "Failed to load artist"
                            }
                            _state.value = _state.value.copy(error = errorMsg, isLoading = false)
                        }
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Unknown error",
                    isLoading = false
                )
            }
        }
    }

    fun toggleFollow() {
        viewModelScope.launch(dispatcher) {
            _state.value = _state.value.copy(
                followedByUser = !_state.value.followedByUser
            )
        }
    }

    private fun sampleArtistAlbums(): List<Album> {
        return listOf(
            Album(id = 1, name = "Greatest Hits", image = null, releaseDate = "2023"),
            Album(id = 2, name = "New Era", image = null, releaseDate = "2024"),
        )
    }
}
