package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single database notification, as returned by the real Laravel endpoint
 * `GET api/v1/notifications` (`Common\Notifications\NotificationController::index`).
 *
 * Shape mirrors the web client's `DatabaseNotification` type exactly (see
 * `common/foundation/resources/client/notifications/database-notification.ts` in the
 * Laravel backend repo): `id` is a UUID string, `data.lines[0].content` is the primary
 * (HTML) message, and `created_at` drives the relative timestamp (e.g. "5 months ago").
 */
@Serializable
data class AppNotification(
    val id: String,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    val type: String,
    val data: NotificationData,
)

@Serializable
data class NotificationData(
    @Serializable(with = ImageUrlSerializer::class) val image: String? = null,
    val lines: List<NotificationLine> = emptyList(),
)

@Serializable
data class NotificationLine(
    val content: String,
    val icon: String? = null,
)
