package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.AppNotification
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class NotificationsPagination(val data: List<AppNotification>)

@Serializable
private data class NotificationsResponse(val pagination: NotificationsPagination)

@Serializable
private data class MarkAsReadRequest(
    val ids: List<String>? = null,
    val markAllAsUnread: Boolean? = null,
)

/**
 * Backs the real Notifications screen, reachable from a bell icon in the top app bar.
 *
 * `GET api/v1/notifications` (`Common\Notifications\NotificationController::index`) -
 * a `simplePaginate`d list of database notifications.
 * `POST api/v1/notifications/mark-as-read` marks either specific [ids] or, despite the
 * confusingly-named `markAllAsUnread` field (kept as-is to match the real backend
 * request contract), *all* notifications as read when that flag is `true`.
 */
open class NotificationsApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    open suspend fun getNotifications(perPage: Int = 10): ApiResult<List<AppNotification>> {
        return try {
            val response = httpClient.get("api/v1/notifications") {
                parameter("perPage", perPage)
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<NotificationsResponse>().pagination.data)
            } else if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                ApiResult.Unauthorized
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    open suspend fun markAsRead(ids: List<String>): ApiResult<Unit> = markNotificationsAsRead(ids = ids)

    open suspend fun markAllAsRead(): ApiResult<Unit> = markNotificationsAsRead(markAllAsUnread = true)

    private suspend fun markNotificationsAsRead(
        ids: List<String>? = null,
        markAllAsUnread: Boolean? = null,
    ): ApiResult<Unit> {
        return try {
            val response = httpClient.post("api/v1/notifications/mark-as-read") {
                contentType(ContentType.Application.Json)
                setBody(MarkAsReadRequest(ids = ids, markAllAsUnread = markAllAsUnread))
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(Unit)
            } else if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                ApiResult.Unauthorized
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
