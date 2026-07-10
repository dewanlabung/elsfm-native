package com.elsfm.mobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.model.UserProfile
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.AccountApi
import com.elsfm.mobile.core.network.api.SessionsApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the account-settings features confirmed to have real, reachable backend
 * support on this app's stateless Bearer-token client:
 *
 * - "Update name and profile image" ([uploadAvatar], [updateName]) - real, via
 *   `POST api/v1/uploads` + `PUT api/v1/users/{id}` ([AccountApi]).
 * - "Sessions" ([loadSessions]) - real, read-only, via `GET api/v1/user-sessions`
 *   ([SessionsApi]). There is intentionally no per-session revoke action here: the
 *   backend has no such endpoint, and its one bulk "logout other sessions" action is
 *   gated behind Fortify's session-cookie password confirmation, which this client
 *   cannot satisfy (see the account-notifications report for the full investigation).
 *
 * "Manage social login", "Update password" and "Two factor authentication" are
 * deliberately NOT implemented here - see the same report for why each was skipped.
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountApi: AccountApi,
    private val sessionsApi: SessionsApi,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(AccountState())
    val state: StateFlow<AccountState> = _state.asStateFlow()

    fun uploadAvatar(userId: Int, bytes: ByteArray, filename: String, mimeType: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(isUploadingAvatar = true, accountError = null) }
            when (val uploadResult = accountApi.uploadAvatar(bytes, filename, mimeType)) {
                is ApiResult.Success -> {
                    val entry = uploadResult.data
                    applyAccountUpdate(
                        accountApi.updateAccountDetails(
                            userId = userId,
                            image = entry.url,
                            imageEntryId = entry.id,
                        ),
                    )
                }
                is ApiResult.NetworkError -> {
                    _state.update {
                        it.copy(isUploadingAvatar = false, accountError = uploadResult.cause.message ?: "Network error")
                    }
                }
                is ApiResult.ValidationError -> {
                    _state.update { it.copy(isUploadingAvatar = false, accountError = "Validation error") }
                }
                is ApiResult.Unauthorized -> {
                    _state.update { it.copy(isUploadingAvatar = false, accountError = "Unauthorized") }
                }
            }
        }
    }

    fun updateName(userId: Int, name: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(isSavingName = true, accountError = null) }
            applyAccountUpdate(accountApi.updateAccountDetails(userId = userId, name = name))
        }
    }

    fun removeAvatar(userId: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(isUploadingAvatar = true, accountError = null) }
            applyAccountUpdate(accountApi.removeAvatar(userId))
        }
    }

    private fun applyAccountUpdate(result: ApiResult<UserProfile>) {
        when (result) {
            is ApiResult.Success -> {
                _state.update {
                    it.copy(account = result.data, isSavingName = false, isUploadingAvatar = false)
                }
            }
            is ApiResult.NetworkError -> {
                _state.update {
                    it.copy(
                        isSavingName = false,
                        isUploadingAvatar = false,
                        accountError = result.cause.message ?: "Network error",
                    )
                }
            }
            is ApiResult.ValidationError -> {
                _state.update {
                    it.copy(isSavingName = false, isUploadingAvatar = false, accountError = "Validation error")
                }
            }
            is ApiResult.Unauthorized -> {
                _state.update {
                    it.copy(isSavingName = false, isUploadingAvatar = false, accountError = "Unauthorized")
                }
            }
        }
    }

    fun loadSessions() {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(isLoadingSessions = true, sessionsError = null) }
            when (val result = sessionsApi.getUserSessions()) {
                is ApiResult.Success -> {
                    _state.update { it.copy(sessions = result.data, isLoadingSessions = false) }
                }
                is ApiResult.NetworkError -> {
                    _state.update {
                        it.copy(isLoadingSessions = false, sessionsError = result.cause.message ?: "Network error")
                    }
                }
                is ApiResult.ValidationError -> {
                    _state.update { it.copy(isLoadingSessions = false, sessionsError = "Validation error") }
                }
                is ApiResult.Unauthorized -> {
                    _state.update { it.copy(isLoadingSessions = false, sessionsError = "Unauthorized") }
                }
            }
        }
    }
}
