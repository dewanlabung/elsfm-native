package com.elsfm.mobile.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.model.Recommendation
import com.elsfm.mobile.core.model.TrendingTrack
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.RecommendationApi
import com.elsfm.mobile.core.network.api.TrendingApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class DiscoveryState(
    val trendingTracks: List<TrendingTrack> = emptyList(),
    val recommendations: List<Recommendation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val trendingApi: TrendingApi,
    private val recommendationApi: RecommendationApi,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(DiscoveryState())
    val state: StateFlow<DiscoveryState> = _state.asStateFlow()

    init {
        loadTrending()
    }

    fun loadTrending() {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = trendingApi.getTrending()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        trendingTracks = result.data,
                        isLoading = false,
                    )
                    loadRecommendations()
                }
                else -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load trending tracks",
                    )
                }
            }
        }
    }

    private fun loadRecommendations() {
        viewModelScope.launch(dispatcherProvider.io) {
            val trackId = _state.value.trendingTracks.firstOrNull()?.track?.id ?: 1
            val result = recommendationApi.getRecommendations(trackId)
            if (result is ApiResult.Success) {
                _state.value = _state.value.copy(recommendations = result.data)
            }
            // Recommendation failures are non-fatal and must not fail the page.
        }
    }
}
