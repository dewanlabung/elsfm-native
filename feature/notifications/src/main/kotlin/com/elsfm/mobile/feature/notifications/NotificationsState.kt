package com.elsfm.mobile.feature.notifications

import com.elsfm.mobile.core.model.AppNotification

data class NotificationsState(
    val notifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
