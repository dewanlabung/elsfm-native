package com.elsfm.mobile.feature.auth

import com.elsfm.mobile.core.model.User

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class FieldErrors(val errors: Map<String, List<String>>) : LoginUiState
    data object InvalidCredentials : LoginUiState
    data object NetworkError : LoginUiState
    data class Success(val user: User) : LoginUiState
}
