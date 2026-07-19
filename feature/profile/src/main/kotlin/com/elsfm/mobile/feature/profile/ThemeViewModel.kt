package com.elsfm.mobile.feature.profile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private fun Int.toColorOrNull(): Color? = if (this == 0) null else Color(this)

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

    private val _customPrimaryColor = MutableStateFlow(themeStore.getCustomPrimaryColor().toColorOrNull())
    val customPrimaryColor: StateFlow<Color?> = _customPrimaryColor.asStateFlow()

    private val _customAccentColor = MutableStateFlow(themeStore.getCustomAccentColor().toColorOrNull())
    val customAccentColor: StateFlow<Color?> = _customAccentColor.asStateFlow()

    private val _customBackgroundColor = MutableStateFlow(themeStore.getCustomBackgroundColor().toColorOrNull())
    val customBackgroundColor: StateFlow<Color?> = _customBackgroundColor.asStateFlow()

    fun toggleTheme() {
        setDarkMode(!_isDarkMode.value)
    }

    fun setDarkMode(isDarkMode: Boolean) {
        themeStore.setDarkMode(isDarkMode)
        _isDarkMode.update { isDarkMode }
    }

    fun setCustomPrimaryColor(color: Color?) {
        val argb = color?.toArgb() ?: 0
        themeStore.setCustomPrimaryColor(argb)
        _customPrimaryColor.update { color }
    }

    fun setCustomAccentColor(color: Color?) {
        val argb = color?.toArgb() ?: 0
        themeStore.setCustomAccentColor(argb)
        _customAccentColor.update { color }
    }

    fun setCustomBackgroundColor(color: Color?) {
        val argb = color?.toArgb() ?: 0
        themeStore.setCustomBackgroundColor(argb)
        _customBackgroundColor.update { color }
    }
}
