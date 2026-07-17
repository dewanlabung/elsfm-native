package com.elsfm.mobile.feature.comments

import com.elsfm.mobile.core.model.Comment

data class CommentsState(
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val isPosting: Boolean = false,
    val draft: String = "",
    val error: String? = null,
)
