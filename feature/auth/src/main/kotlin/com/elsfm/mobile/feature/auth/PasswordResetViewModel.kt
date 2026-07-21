package com.elsfm.mobile.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PasswordResetState(
    val email: String = "",
    val token: String? = null,
    val password: String = "",
    val passwordConfirm: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSubmitted: Boolean = false,
)

sealed class PasswordResetEvent {
    data class EmailChanged(val email: String) : PasswordResetEvent()
    data class PasswordChanged(val password: String) : PasswordResetEvent()
    data class PasswordConfirmChanged(val password: String) : PasswordResetEvent()
    object ResetClicked : PasswordResetEvent()
}

@HiltViewModel
class PasswordResetViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PasswordResetState())
    val state = _state.asStateFlow()

    fun initializeWithDeepLink(token: String, email: String) {
        _state.value = _state.value.copy(token = token, email = email)
    }

    fun onEvent(event: PasswordResetEvent) {
        when (event) {
            is PasswordResetEvent.EmailChanged -> {
                _state.value = _state.value.copy(email = event.email)
            }
            is PasswordResetEvent.PasswordChanged -> {
                _state.value = _state.value.copy(password = event.password)
            }
            is PasswordResetEvent.PasswordConfirmChanged -> {
                _state.value = _state.value.copy(passwordConfirm = event.password)
            }
            PasswordResetEvent.ResetClicked -> {
                if (_state.value.token != null) {
                    completePasswordReset()
                } else {
                    requestPasswordReset()
                }
            }
        }
    }

    private fun requestPasswordReset() {
        viewModelScope.launch {
            val currentState = _state.value
            _state.value = currentState.copy(isLoading = true, error = null)

            if (currentState.email.isEmpty()) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Please enter your email address"
                )
                return@launch
            }

            when (val result = authRepository.requestPasswordReset(currentState.email)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false, isSubmitted = true)
                }
                is ApiResult.ValidationError -> {
                    val errorMessages = result.fields.values.flatten().joinToString(", ")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = errorMessages.ifEmpty { "Validation error" }
                    )
                }
                is ApiResult.NetworkError -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Network error. Please check your connection."
                    )
                }
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Email not found. Please check the address or sign up for a new account."
                    )
                }
            }
        }
    }

    private fun completePasswordReset() {
        viewModelScope.launch {
            val currentState = _state.value
            _state.value = currentState.copy(isLoading = true, error = null)

            if (currentState.password.length < 8) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Password must be at least 8 characters"
                )
                return@launch
            }

            if (currentState.password != currentState.passwordConfirm) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Passwords do not match"
                )
                return@launch
            }

            when (val result = authRepository.resetPassword(
                email = currentState.email,
                token = currentState.token!!,
                password = currentState.password,
                passwordConfirm = currentState.passwordConfirm
            )) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false, isSubmitted = true)
                }
                is ApiResult.ValidationError -> {
                    val errorMessages = result.fields.values.flatten().joinToString(", ")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = errorMessages.ifEmpty { "Validation error" }
                    )
                }
                is ApiResult.NetworkError -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Network error. Please check your connection."
                    )
                }
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Invalid or expired reset link. Please request a new one."
                    )
                }
            }
        }
    }
}
