package com.elsfm.mobile.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onLoginClicked(email: String, password: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.value = LoginUiState.Loading
            _state.value = when (val result = authRepository.login(email, password)) {
                is ApiResult.Success -> LoginUiState.Success(result.data)
                is ApiResult.ValidationError -> LoginUiState.FieldErrors(result.fields)
                is ApiResult.Unauthorized -> LoginUiState.InvalidCredentials
                is ApiResult.NetworkError -> LoginUiState.NetworkError
            }
        }
    }
}
