package com.elsfm.mobile.feature.profile

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "elsfm_theme_prefs"
private const val KEY_DARK_MODE = "dark_mode"

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
}
