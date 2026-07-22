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
    val isAutoplayEnabled: Boolean = true,
    val isVolumeNormalizationEnabled: Boolean = false,
    val isSkipSilenceEnabled: Boolean = false,
    val isPersistentQueueEnabled: Boolean = true,
    val isResumePlaybackEnabled: Boolean = true,
    val isPrivateSession: Boolean = false,
    val isOfflineModeEnabled: Boolean = false,
    val isWifiAutoCacheEnabled: Boolean = false,
    val isKeepScreenOnEnabled: Boolean = false,
    val isSearchHistoryPaused: Boolean = false,
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
            isAutoplayEnabled = sessionPreferences.isAutoplayEnabled,
            isVolumeNormalizationEnabled = sessionPreferences.isVolumeNormalizationEnabled,
            isSkipSilenceEnabled = sessionPreferences.isSkipSilenceEnabled,
            isPersistentQueueEnabled = sessionPreferences.isPersistentQueueEnabled,
            isResumePlaybackEnabled = sessionPreferences.isResumePlaybackEnabled,
            isPrivateSession = sessionPreferences.isPrivateSession,
            isOfflineModeEnabled = sessionPreferences.isOfflineModeEnabled,
            isWifiAutoCacheEnabled = sessionPreferences.isWifiAutoCacheEnabled,
            isKeepScreenOnEnabled = sessionPreferences.isKeepScreenOnEnabled,
            isSearchHistoryPaused = sessionPreferences.isSearchHistoryPaused,
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

    fun toggleAutoplay() {
        val newValue = !_state.value.isAutoplayEnabled
        sessionPreferences.isAutoplayEnabled = newValue
        _state.value = _state.value.copy(isAutoplayEnabled = newValue)
    }

    fun toggleVolumeNormalization() {
        val newValue = !_state.value.isVolumeNormalizationEnabled
        sessionPreferences.isVolumeNormalizationEnabled = newValue
        _state.value = _state.value.copy(isVolumeNormalizationEnabled = newValue)
    }

    fun toggleSkipSilence() {
        val newValue = !_state.value.isSkipSilenceEnabled
        sessionPreferences.isSkipSilenceEnabled = newValue
        _state.value = _state.value.copy(isSkipSilenceEnabled = newValue)
    }

    fun togglePersistentQueue() {
        val newValue = !_state.value.isPersistentQueueEnabled
        sessionPreferences.isPersistentQueueEnabled = newValue
        _state.value = _state.value.copy(isPersistentQueueEnabled = newValue)
    }

    fun toggleResumePlayback() {
        val newValue = !_state.value.isResumePlaybackEnabled
        sessionPreferences.isResumePlaybackEnabled = newValue
        _state.value = _state.value.copy(isResumePlaybackEnabled = newValue)
    }

    fun toggleKeepScreenOn() {
        val newValue = !_state.value.isKeepScreenOnEnabled
        sessionPreferences.isKeepScreenOnEnabled = newValue
        _state.value = _state.value.copy(isKeepScreenOnEnabled = newValue)
    }

    fun toggleSearchHistoryPaused() {
        val newValue = !_state.value.isSearchHistoryPaused
        sessionPreferences.isSearchHistoryPaused = newValue
        _state.value = _state.value.copy(isSearchHistoryPaused = newValue)
    }

    fun toggleOfflineMode() {
        val newValue = !_state.value.isOfflineModeEnabled
        sessionPreferences.isOfflineModeEnabled = newValue
        _state.value = _state.value.copy(isOfflineModeEnabled = newValue)
    }

    fun toggleWifiAutoCache() {
        val newValue = !_state.value.isWifiAutoCacheEnabled
        sessionPreferences.isWifiAutoCacheEnabled = newValue
        _state.value = _state.value.copy(isWifiAutoCacheEnabled = newValue)
    }
}
