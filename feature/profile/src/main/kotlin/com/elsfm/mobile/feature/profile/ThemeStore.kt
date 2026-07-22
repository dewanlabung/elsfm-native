package com.elsfm.mobile.feature.profile

/**
 * Storage abstraction for the user's dark/light theme preference and accent color. See
 * [ThemePreferences] for the SharedPreferences-backed implementation.
 */
interface ThemeStore {
    fun isDarkMode(): Boolean
    fun setDarkMode(isDarkMode: Boolean)
    fun accentColorHex(): String
    fun setAccentColorHex(hex: String)
}
