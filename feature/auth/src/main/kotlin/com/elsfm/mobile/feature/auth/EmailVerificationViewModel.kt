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

data class EmailVerificationState(
    val code: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isVerified: Boolean = false,
)

sealed class EmailVerificationEvent {
    data class CodeChanged(val code: String) : EmailVerificationEvent()
    object VerifyClicked : EmailVerificationEvent()
}

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(EmailVerificationState())
    val state = _state.asStateFlow()

    fun onEvent(event: EmailVerificationEvent) {
        when (event) {
            is EmailVerificationEvent.CodeChanged -> {
                // Accept only digits, max 6 characters
                val digits = event.code.filter { it.isDigit() }.take(6)
                _state.value = _state.value.copy(code = digits, error = null)
            }
            EmailVerificationEvent.VerifyClicked -> verify()
        }
    }

    private fun verify() {
        val currentCode = _state.value.code
        if (currentCode.length != 6) {
            _state.value = _state.value.copy(error = "Please enter the 6-digit code from your email")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            when (val result = authRepository.verifyEmail(currentCode)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false, isVerified = true)
                }
                is ApiResult.ValidationError -> {
                    val errorMessages = result.fields.values.flatten().joinToString(", ")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = errorMessages.ifEmpty { "Invalid code. Please try again." }
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
                        error = "Invalid or expired code. Please request a new one."
                    )
                }
            }
        }
    }
}
