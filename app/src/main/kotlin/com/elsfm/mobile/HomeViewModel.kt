package com.elsfm.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.TrackListApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PLACEHOLDER_PLAYLIST_ID = 8

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val trackListApi: TrackListApi,
) : ViewModel() {
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    init {
        viewModelScope.launch {
            val result = trackListApi.getPlaylistTracks(PLACEHOLDER_PLAYLIST_ID)
            if (result is ApiResult.Success) {
                _tracks.value = result.data
            }
        }
    }
}
