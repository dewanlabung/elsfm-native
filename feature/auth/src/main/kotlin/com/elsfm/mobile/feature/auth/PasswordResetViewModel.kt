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
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSubmitted: Boolean = false,
)

sealed class PasswordResetEvent {
    data class EmailChanged(val email: String) : PasswordResetEvent()
    object ResetClicked : PasswordResetEvent()
}

@HiltViewModel
class PasswordResetViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PasswordResetState())
    val state = _state.asStateFlow()

    fun onEvent(event: PasswordResetEvent) {
        when (event) {
            is PasswordResetEvent.EmailChanged -> {
                _state.value = _state.value.copy(email = event.email)
            }
            PasswordResetEvent.ResetClicked -> {
                resetPassword()
            }
        }
    }

    private fun resetPassword() {
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

            _state.value = _state.value.copy(
                isLoading = false,
                isSubmitted = true
            )
        }
    }
}
