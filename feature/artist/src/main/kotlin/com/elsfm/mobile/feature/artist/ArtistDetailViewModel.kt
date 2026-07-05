package com.elsfm.mobile.feature.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ArtistApi
import dagger.hilt.android.lifecycle.HiltViewModel
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
            loadArtist(artistId)
            loadTracks(artistId)
        }
    }

    private fun loadArtist(id: Int) {
        viewModelScope.launch(dispatcher) {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = artistApi.getArtist(id)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(artist = result.data, isLoading = false)
                }
                is ApiResult.NetworkError -> {
                    _state.value = _state.value.copy(
                        error = result.cause.message ?: "Network error",
                        isLoading = false
                    )
                }
                is ApiResult.ValidationError -> {
                    _state.value = _state.value.copy(
                        error = "Validation error",
                        isLoading = false
                    )
                }
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(
                        error = "Unauthorized",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadTracks(id: Int) {
        viewModelScope.launch(dispatcher) {
            when (val result = artistApi.getArtistTracks(id)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(tracks = result.data)
                }
                is ApiResult.NetworkError -> {
                    _state.value = _state.value.copy(
                        error = result.cause.message ?: "Failed to load tracks"
                    )
                }
                is ApiResult.ValidationError -> {
                    _state.value = _state.value.copy(error = "Validation error")
                }
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(error = "Unauthorized")
                }
            }
        }
    }
}
