package com.elsfm.mobile.feature.comments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.CommentApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal const val TRACK_ID_ARG = "trackId"
private const val COMMENTABLE_TYPE_TRACK = "track"

/**
 * Comments for a single track's dedicated Comments screen, reached from
 * [com.elsfm.mobile.core.designsystem.TrackContextMenu]'s "Comments" item. Uses the same
 * `CommentApi` already used inline by `AlbumViewModel` for album comments - here
 * `commentable_type` is fixed to `"track"`.
 */
@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val commentApi: CommentApi,
    private val savedStateHandle: SavedStateHandle,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val dispatcher = dispatcherProvider.io
    private val trackId: Int = requireNotNull(savedStateHandle[TRACK_ID_ARG]) { "trackId is required" }

    private val _state = MutableStateFlow(CommentsState())
    val state: StateFlow<CommentsState> = _state.asStateFlow()

    init {
        loadComments()
    }

    fun retryLoad() {
        loadComments()
    }

    private fun loadComments() {
        viewModelScope.launch(dispatcher) {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = commentApi.getComments(COMMENTABLE_TYPE_TRACK, trackId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(comments = result.data, isLoading = false)
                }
                is ApiResult.NetworkError -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.cause.message ?: "Failed to load comments",
                    )
                }
                is ApiResult.ValidationError -> {
                    _state.value = _state.value.copy(isLoading = false, error = "Validation error")
                }
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(isLoading = false, error = "Unauthorized")
                }
            }
        }
    }

    fun onDraftChanged(text: String) {
        _state.value = _state.value.copy(draft = text)
    }

    fun postComment() {
        val content = _state.value.draft.trim()
        if (content.isEmpty()) return
        viewModelScope.launch(dispatcher) {
            _state.value = _state.value.copy(isPosting = true)
            when (val result = commentApi.postComment(COMMENTABLE_TYPE_TRACK, trackId, content)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        comments = listOf(result.data) + _state.value.comments,
                        draft = "",
                        isPosting = false,
                    )
                }
                else -> {
                    _state.value = _state.value.copy(
                        isPosting = false,
                        error = "Failed to post comment",
                    )
                }
            }
        }
    }
}
