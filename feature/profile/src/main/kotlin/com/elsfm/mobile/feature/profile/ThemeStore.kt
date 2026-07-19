package com.elsfm.mobile.feature.profile

/**
 * Storage abstraction for the user's dark/light theme preference. See
 * [ThemePreferences] for the SharedPreferences-backed implementation.
 */
interface ThemeStore {
    fun isDarkMode(): Boolean
    fun setDarkMode(isDarkMode: Boolean)

    fun getCustomPrimaryColor(): Int
    fun setCustomPrimaryColor(argb: Int)

    fun getCustomAccentColor(): Int
    fun setCustomAccentColor(argb: Int)

    fun getCustomBackgroundColor(): Int
    fun setCustomBackgroundColor(argb: Int)
}
