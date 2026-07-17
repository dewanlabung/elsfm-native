package com.elsfm.mobile.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.feature.auth.data.AuthRepository
import com.elsfm.mobile.feature.auth.data.GoogleSignInService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val rememberMe: Boolean = false
)

sealed class LoginEvent {
    data class EmailChanged(val email: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    data class RememberMeChanged(val remember: Boolean) : LoginEvent()
    object LoginClicked : LoginEvent()
    data class GoogleSignInSucceeded(val accountEmail: String) : LoginEvent()
    data class GoogleSignInFailed(val message: String) : LoginEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleSignInService: GoogleSignInService,
    private val passwordSaver: PasswordSaver,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    fun googleSignInClient() = googleSignInService.signInClient

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> {
                _state.value = _state.value.copy(email = event.email)
            }
            is LoginEvent.PasswordChanged -> {
                _state.value = _state.value.copy(password = event.password)
            }
            is LoginEvent.RememberMeChanged -> {
                _state.value = _state.value.copy(rememberMe = event.remember)
            }
            LoginEvent.LoginClicked -> {
                login()
            }
            is LoginEvent.GoogleSignInSucceeded -> {
                loginWithGoogle(event.accountEmail)
            }
            is LoginEvent.GoogleSignInFailed -> {
                _state.value = _state.value.copy(isLoading = false, error = event.message)
            }
        }
    }

    private fun login() {
        viewModelScope.launch {
            val currentState = _state.value
            _state.value = currentState.copy(isLoading = true, error = null)

            when (val result = authRepository.login(currentState.email, currentState.password)) {
                is ApiResult.Success -> {
                    passwordSaver.save(currentState.email, currentState.password)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isLoggedIn = true
                    )
                }
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Invalid email or password"
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
            }
        }
    }

    private fun loginWithGoogle(accountEmail: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = try {
                val accessToken = googleSignInService.fetchAccessToken(accountEmail)
                authRepository.loginWithGoogle(accessToken)
            } catch (e: Exception) {
                ApiResult.NetworkError(e)
            }

            when (result) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false, isLoggedIn = true)
                }
                is ApiResult.Unauthorized -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Google sign-in failed"
                    )
                }
                is ApiResult.ValidationError -> {
                    val errorMessages = result.fields.values.flatten().joinToString(", ")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = errorMessages.ifEmpty { "Google sign-in failed" }
                    )
                }
                is ApiResult.NetworkError -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Google sign-in failed: ${result.cause.message ?: "network error"}"
                    )
                }
            }
        }
    }
}
