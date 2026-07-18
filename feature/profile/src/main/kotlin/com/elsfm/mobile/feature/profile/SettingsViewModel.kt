package com.elsfm.mobile.feature.profile

import androidx.lifecycle.ViewModel
import com.elsfm.mobile.core.media.SessionPreferences
import com.elsfm.mobile.core.media.ShakePreferences
import com.elsfm.mobile.core.media.ShakeSensitivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SettingsState(
    val shakeEnabled: Boolean = true,
    val shakeSensitivity: ShakeSensitivity = ShakeSensitivity.MEDIUM,
    val isPrivateSession: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val shakePreferences: ShakePreferences,
    private val sessionPreferences: SessionPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsState(
            shakeEnabled = shakePreferences.isEnabled,
            shakeSensitivity = shakePreferences.sensitivity,
            isPrivateSession = sessionPreferences.isPrivateSession,
        )
    )
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun toggleShake() {
        val newValue = !_state.value.shakeEnabled
        shakePreferences.isEnabled = newValue
        _state.value = _state.value.copy(shakeEnabled = newValue)
    }

    fun setSensitivity(sensitivity: ShakeSensitivity) {
        shakePreferences.sensitivity = sensitivity
        _state.value = _state.value.copy(shakeSensitivity = sensitivity)
    }

    fun togglePrivateSession() {
        val newValue = !_state.value.isPrivateSession
        sessionPreferences.isPrivateSession = newValue
        _state.value = _state.value.copy(isPrivateSession = newValue)
    }
}
