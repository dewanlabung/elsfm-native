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

data class SignupState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSignedUp: Boolean = false,
    val acceptTerms: Boolean = false,
    val acceptPrivacy: Boolean = false,
)

sealed class SignupEvent {
    data class EmailChanged(val email: String) : SignupEvent()
    data class PasswordChanged(val password: String) : SignupEvent()
    data class ConfirmPasswordChanged(val password: String) : SignupEvent()
    data class AcceptTermsChanged(val accept: Boolean) : SignupEvent()
    data class AcceptPrivacyChanged(val accept: Boolean) : SignupEvent()
    object SignupClicked : SignupEvent()
}

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SignupState())
    val state = _state.asStateFlow()

    fun onEvent(event: SignupEvent) {
        when (event) {
            is SignupEvent.EmailChanged -> {
                _state.value = _state.value.copy(email = event.email)
            }
            is SignupEvent.PasswordChanged -> {
                _state.value = _state.value.copy(password = event.password)
            }
            is SignupEvent.ConfirmPasswordChanged -> {
                _state.value = _state.value.copy(confirmPassword = event.password)
            }
            is SignupEvent.AcceptTermsChanged -> {
                _state.value = _state.value.copy(acceptTerms = event.accept)
            }
            is SignupEvent.AcceptPrivacyChanged -> {
                _state.value = _state.value.copy(acceptPrivacy = event.accept)
            }
            SignupEvent.SignupClicked -> {
                signup()
            }
        }
    }

    private fun signup() {
        val currentState = _state.value

        if (currentState.password != currentState.confirmPassword) {
            _state.value = currentState.copy(error = "Passwords do not match")
            return
        }

        if (!currentState.acceptTerms || !currentState.acceptPrivacy) {
            _state.value = currentState.copy(error = "You must accept Terms and Privacy Policy")
            return
        }

        viewModelScope.launch {
            _state.value = currentState.copy(isLoading = true, error = null)

            when (val result = authRepository.register(
                currentState.email,
                currentState.password
            )) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isSignedUp = true
                    )
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
                else -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Signup failed"
                    )
                }
            }
        }
    }
}
