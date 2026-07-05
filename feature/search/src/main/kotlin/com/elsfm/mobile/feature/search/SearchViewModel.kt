package com.elsfm.mobile.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.model.SearchResult
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.SearchApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchApi: SearchApi,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    fun search(query: String) {
        _state.value = _state.value.copy(query = query, isLoading = true, error = null)
        viewModelScope.launch {
            val result = searchApi.search(query)
            if (result is ApiResult.Success) {
                _state.value = _state.value.copy(results = result.data, isLoading = false)
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Search failed"
                )
            }
        }
    }

    fun clearSearch() {
        _state.value = SearchState()
    }
}
