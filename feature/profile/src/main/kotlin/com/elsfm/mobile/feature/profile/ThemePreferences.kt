package com.elsfm.mobile.feature.profile

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "elsfm_theme_prefs"
private const val KEY_DARK_MODE = "dark_mode"
private const val KEY_CUSTOM_PRIMARY = "custom_primary_color"
private const val KEY_CUSTOM_ACCENT = "custom_accent_color"
private const val KEY_CUSTOM_BACKGROUND = "custom_background_color"

/**
 * SharedPreferences-backed store for the user's dark/light theme choice.
 */
@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext context: Context,
) : ThemeStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    override fun setDarkMode(isDarkMode: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, isDarkMode).apply()
    }

    override fun getCustomPrimaryColor(): Int = prefs.getInt(KEY_CUSTOM_PRIMARY, 0)
    override fun setCustomPrimaryColor(argb: Int) { prefs.edit().putInt(KEY_CUSTOM_PRIMARY, argb).apply() }

    override fun getCustomAccentColor(): Int = prefs.getInt(KEY_CUSTOM_ACCENT, 0)
    override fun setCustomAccentColor(argb: Int) { prefs.edit().putInt(KEY_CUSTOM_ACCENT, argb).apply() }

    override fun getCustomBackgroundColor(): Int = prefs.getInt(KEY_CUSTOM_BACKGROUND, 0)
    override fun setCustomBackgroundColor(argb: Int) { prefs.edit().putInt(KEY_CUSTOM_BACKGROUND, argb).apply() }
}
