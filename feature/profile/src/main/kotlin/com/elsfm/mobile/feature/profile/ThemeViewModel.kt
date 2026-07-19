package com.elsfm.mobile.feature.profile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * App-wide dark/light theme state, backed by [ThemePreferences]. Shared by the root
 * theme composable (to decide the active color scheme) and the Profile/Account screen
 * (to expose a toggle to the user).
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeStore: ThemeStore,
) : ViewModel() {
    private val _isDarkMode = MutableStateFlow(themeStore.isDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleTheme() {
        setDarkMode(!_isDarkMode.value)
    }

    fun setDarkMode(isDarkMode: Boolean) {
        themeStore.setDarkMode(isDarkMode)
        _isDarkMode.update { isDarkMode }
    }
}
