package com.elsfm.mobile.core.media

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "elsfm_shake_prefs"
private const val KEY_ENABLED = "shake_to_skip_enabled"
private const val KEY_SENSITIVITY = "shake_sensitivity"

/**
 * Persists the user's shake-to-skip preferences across app restarts.
 *
 * Backed by SharedPreferences; safe to inject anywhere as a singleton.
 * Default: enabled at MEDIUM sensitivity.
 */
@Singleton
class ShakePreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var sensitivity: ShakeSensitivity
        get() = runCatching {
            ShakeSensitivity.valueOf(prefs.getString(KEY_SENSITIVITY, null) ?: "")
        }.getOrDefault(ShakeSensitivity.MEDIUM)
        set(value) = prefs.edit().putString(KEY_SENSITIVITY, value.name).apply()
}
