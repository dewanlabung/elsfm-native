package com.elsfm.mobile.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.NotificationsApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the real Notifications screen, reachable from a bell icon in the top app bar
 * (see the account-notifications report for the exact suggested nav wiring). Real
 * backend: `GET api/v1/notifications` + `POST api/v1/notifications/mark-as-read`
 * (`Common\Notifications\NotificationController`).
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationsApi: NotificationsApi,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = notificationsApi.getNotifications()) {
                is ApiResult.Success -> {
                    _state.update { it.copy(notifications = result.data, isLoading = false) }
                }
                is ApiResult.NetworkError -> {
                    _state.update { it.copy(isLoading = false, error = result.cause.message ?: "Network error") }
                }
                is ApiResult.ValidationError -> {
                    _state.update { it.copy(isLoading = false, error = "Validation error") }
                }
                is ApiResult.Unauthorized -> {
                    _state.update { it.copy(isLoading = false, error = "Unauthorized") }
                }
            }
        }
    }

    fun markAsRead(id: String) {
        val alreadyRead = _state.value.notifications.find { it.id == id }?.readAt != null
        if (alreadyRead) return

        viewModelScope.launch(dispatcherProvider.io) {
            when (notificationsApi.markAsRead(listOf(id))) {
                is ApiResult.Success -> {
                    _state.update { current ->
                        current.copy(
                            notifications = current.notifications.map { notification ->
                                if (notification.id == id) {
                                    notification.copy(readAt = java.time.Instant.now().toString())
                                } else {
                                    notification
                                }
                            },
                        )
                    }
                }
                is ApiResult.NetworkError, is ApiResult.ValidationError, is ApiResult.Unauthorized -> {
                    // Marking as read is a non-critical background action; failures are ignored
                    // rather than surfaced as a blocking error on the whole list.
                }
            }
        }
    }
}
