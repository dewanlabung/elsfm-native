package com.elsfm.mobile.feature.comments

import com.elsfm.mobile.core.model.Comment

data class CommentsState(
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val isPosting: Boolean = false,
    val draft: String = "",
    val error: String? = null,
    val currentUserId: Int? = null,
    val editingCommentId: Int? = null,
    val editDraft: String = "",
    val deletingCommentIds: Set<Int> = emptySet(),
    val updatingCommentId: Int? = null,
)
