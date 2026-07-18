package com.elsfm.mobile.core.media

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "elsfm_session_prefs"
private const val KEY_PRIVATE_SESSION = "private_session_enabled"
private const val KEY_AUTOPLAY = "autoplay_enabled"
private const val KEY_VOLUME_NORMALIZATION = "volume_normalization_enabled"

/**
 * Persists session-level behavior flags across app restarts.
 *
 * Backed by SharedPreferences; safe to inject anywhere as a singleton.
 */
@Singleton
class SessionPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isPrivateSession: Boolean
        get() = prefs.getBoolean(KEY_PRIVATE_SESSION, false)
        set(value) = prefs.edit().putBoolean(KEY_PRIVATE_SESSION, value).apply()

    /** When true, the player appends locally-recommended tracks when the queue ends. */
    var isAutoplayEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTOPLAY, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTOPLAY, value).apply()

    /** When true, a LoudnessEnhancer effect is attached to the audio session. */
    var isVolumeNormalizationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOLUME_NORMALIZATION, false)
        set(value) = prefs.edit().putBoolean(KEY_VOLUME_NORMALIZATION, value).apply()
}
