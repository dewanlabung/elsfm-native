package com.elsfm.mobile.feature.comments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.CommentApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal const val TRACK_ID_ARG = "trackId"
private const val COMMENTABLE_TYPE_TRACK = "track"

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val commentApi: CommentApi,
    private val userDao: UserDao,
    private val savedStateHandle: SavedStateHandle,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val dispatcher = dispatcherProvider.io
    private val trackId: Int = requireNotNull(savedStateHandle[TRACK_ID_ARG]) { "trackId is required" }

    private val _state = MutableStateFlow(CommentsState())
    val state: StateFlow<CommentsState> = _state.asStateFlow()

    init {
        loadCurrentUser()
        loadComments()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch(dispatcher) {
            val user = userDao.get()
            _state.update { it.copy(currentUserId = user?.id) }
        }
    }

    fun retryLoad() = loadComments()

    private fun loadComments() {
        viewModelScope.launch(dispatcher) {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = commentApi.getComments(COMMENTABLE_TYPE_TRACK, trackId)) {
                is ApiResult.Success -> _state.update { it.copy(comments = result.data, isLoading = false) }
                is ApiResult.NetworkError -> _state.update {
                    it.copy(isLoading = false, error = result.cause.message ?: "Failed to load comments")
                }
                is ApiResult.ValidationError -> _state.update { it.copy(isLoading = false, error = "Validation error") }
                is ApiResult.Unauthorized -> _state.update { it.copy(isLoading = false, error = "Unauthorized") }
            }
        }
    }

    fun onDraftChanged(text: String) = _state.update { it.copy(draft = text) }

    fun postComment() {
        val content = _state.value.draft.trim()
        if (content.isEmpty()) return
        viewModelScope.launch(dispatcher) {
            _state.update { it.copy(isPosting = true) }
            when (val result = commentApi.postComment(COMMENTABLE_TYPE_TRACK, trackId, content)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        comments = listOf(result.data) + it.comments,
                        draft = "",
                        isPosting = false,
                    )
                }
                else -> _state.update { it.copy(isPosting = false, error = "Failed to post comment") }
            }
        }
    }

    fun startEditing(commentId: Int) {
        val comment = _state.value.comments.find { it.id == commentId } ?: return
        _state.update { it.copy(editingCommentId = commentId, editDraft = comment.content ?: "") }
    }

    fun onEditDraftChanged(text: String) = _state.update { it.copy(editDraft = text) }

    fun cancelEditing() = _state.update { it.copy(editingCommentId = null, editDraft = "") }

    fun submitEdit() {
        val commentId = _state.value.editingCommentId ?: return
        val content = _state.value.editDraft.trim()
        if (content.isEmpty()) return
        viewModelScope.launch(dispatcher) {
            _state.update { it.copy(updatingCommentId = commentId) }
            when (val result = commentApi.updateComment(commentId, content)) {
                is ApiResult.Success -> _state.update { s ->
                    s.copy(
                        comments = s.comments.map { c -> if (c.id == commentId) result.data else c },
                        editingCommentId = null,
                        editDraft = "",
                        updatingCommentId = null,
                    )
                }
                else -> _state.update { it.copy(updatingCommentId = null, error = "Failed to update comment") }
            }
        }
    }

    fun deleteComment(commentId: Int) {
        viewModelScope.launch(dispatcher) {
            _state.update { it.copy(deletingCommentIds = it.deletingCommentIds + commentId) }
            when (commentApi.deleteComment(commentId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        comments = it.comments.filterNot { c -> c.id == commentId },
                        deletingCommentIds = it.deletingCommentIds - commentId,
                    )
                }
                else -> _state.update {
                    it.copy(
                        deletingCommentIds = it.deletingCommentIds - commentId,
                        error = "Failed to delete comment",
                    )
                }
            }
        }
    }
}
