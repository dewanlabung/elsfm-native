package com.elsfm.mobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.AccountApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    // Field-level errors keyed by backend field name (current_password, password,
    // password_confirmation) or by the client-side key "confirm".
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSuccess: Boolean = false,
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val accountApi: AccountApi,
    private val userDao: UserDao,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordState())
    val state: StateFlow<ChangePasswordState> = _state.asStateFlow()

    fun onCurrentPasswordChanged(value: String) {
        _state.update { it.copy(currentPassword = value, error = null, fieldErrors = emptyMap()) }
    }

    fun onNewPasswordChanged(value: String) {
        _state.update { it.copy(newPassword = value, error = null, fieldErrors = emptyMap()) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _state.update { it.copy(confirmPassword = value, error = null, fieldErrors = emptyMap()) }
    }

    fun submit() {
        val s = _state.value

        // Client-side guard: passwords must match before hitting the network.
        if (s.newPassword != s.confirmPassword) {
            _state.update { it.copy(fieldErrors = mapOf("password_confirmation" to "Passwords do not match")) }
            return
        }
        if (s.newPassword.length < 8) {
            _state.update { it.copy(fieldErrors = mapOf("password" to "Password must be at least 8 characters")) }
            return
        }

        viewModelScope.launch(dispatcherProvider.io) {
            val userId = userDao.get()?.id
            if (userId == null) {
                _state.update { it.copy(error = "Not signed in") }
                return@launch
            }

            _state.update { it.copy(isLoading = true, error = null, fieldErrors = emptyMap()) }

            when (val result = accountApi.changePassword(userId, s.currentPassword, s.newPassword)) {
                is ApiResult.Success -> {
                    // Reset the form on success so it's clean if the user navigates back.
                    _state.update { ChangePasswordState(isSuccess = true) }
                }
                is ApiResult.ValidationError -> {
                    // Flatten backend validation lists to single strings for each field.
                    val fieldErrors = result.fields.mapValues { (_, msgs) -> msgs.firstOrNull().orEmpty() }
                    _state.update { it.copy(isLoading = false, fieldErrors = fieldErrors) }
                }
                is ApiResult.NetworkError -> {
                    _state.update { it.copy(isLoading = false, error = "Network error. Please try again.") }
                }
                is ApiResult.Unauthorized -> {
                    _state.update { it.copy(isLoading = false, error = "Session expired. Please log in again.") }
                }
            }
        }
    }
}
