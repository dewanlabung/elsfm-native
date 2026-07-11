package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Comment
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class CommentsPagination(val data: List<Comment> = emptyList())

@Serializable
private data class CommentsResponse(val pagination: CommentsPagination)

@Serializable
private data class CreateCommentRequest(
    val content: String,
    @SerialName("commentable_type") val commentableType: String,
    @SerialName("commentable_id") val commentableId: Int,
)

@Serializable
private data class CreateCommentResponse(val comment: Comment)

/**
 * Backed by the real Laravel comments feature (`common/foundation/routes/api.php`):
 * `GET api/v1/commentable/comments?commentable_type=X&commentable_id=Y`
 * (`CommentableController::index`, via `PaginateModelComments` - top-level comments only,
 * no reply-threading support here) and `POST api/v1/comment`
 * (`CommentController::store`, apiResource route).
 */
class CommentApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun getComments(commentableType: String, commentableId: Int): ApiResult<List<Comment>> {
        return try {
            val response = httpClient.get("api/v1/commentable/comments") {
                parameter("commentable_type", commentableType)
                parameter("commentable_id", commentableId)
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<CommentsResponse>().pagination.data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend fun postComment(commentableType: String, commentableId: Int, content: String): ApiResult<Comment> {
        return try {
            val response = httpClient.post("api/v1/comment") {
                contentType(ContentType.Application.Json)
                setBody(CreateCommentRequest(content, commentableType, commentableId))
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<CreateCommentResponse>().comment)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
