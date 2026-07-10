package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single active login session/device, as returned by the real Laravel endpoint
 * `GET api/v1/user-sessions` (`Common\Auth\Controllers\UserSessionsController::index`).
 *
 * Note: the backend only exposes this read-only list plus one bulk
 * `POST user-sessions/logout-other` action - there is no per-session revoke endpoint,
 * and the bulk logout action is gated behind Fortify's session-based password
 * confirmation (`password.confirm`), which this app's stateless Bearer-token client
 * cannot satisfy. See feature/profile's AccountViewModel/SessionsSection for details.
 */
@Serializable
data class UserSessionInfo(
    val id: Int,
    val platform: String? = null,
    val browser: String? = null,
    val device: String? = null,
    val country: String? = null,
    val city: String? = null,
    @SerialName("ip_address") val ipAddress: String? = null,
    @SerialName("is_current_device") val isCurrentDevice: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null,
)
