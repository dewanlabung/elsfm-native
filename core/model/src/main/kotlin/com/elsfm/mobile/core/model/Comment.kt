package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: Int,
    val content: String? = null,
    @SerialName("user_id") val userId: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val user: CommentUser? = null,
)

@Serializable
data class CommentUser(
    val id: Int,
    val name: String,
    @Serializable(with = ImageUrlSerializer::class) val image: String? = null,
)
