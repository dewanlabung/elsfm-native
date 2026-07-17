package com.elsfm.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.model.User
import com.elsfm.mobile.core.network.auth.SessionEvent
import com.elsfm.mobile.core.network.auth.SessionManager
import com.elsfm.mobile.feature.auth.data.AuthRepository
import com.elsfm.mobile.feature.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StartDestinationState {
    data object Loading : StartDestinationState
    data class Resolved(val route: String, val restoredUser: User?) : StartDestinationState
}

@HiltViewModel
class StartDestinationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val playerController: PlayerController,
) : ViewModel() {

    private val _state = MutableStateFlow<StartDestinationState>(StartDestinationState.Loading)
    val state: StateFlow<StartDestinationState> = _state.asStateFlow()

    val sessionEvents: SharedFlow<SessionEvent> = sessionManager.events

    init {
        viewModelScope.launch {
            val restoredUser = authRepository.restoredUser()
            _state.value = StartDestinationState.Resolved(
                route = if (restoredUser != null) "home" else "login",
                restoredUser = restoredUser,
            )
        }
    }

    fun logout() {
        // Stop first so a track already playing doesn't keep playing under the
        // login screen or the next (possibly different) signed-in session.
        playerController.stop()
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
