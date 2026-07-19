package com.elsfm.mobile.core.media

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class DownloadQuality(val kbps: Int) {
    LOW(128),
    MEDIUM(256),
    HIGH(320),
}

private const val PREFS_NAME = "elsfm_session_prefs"
private const val KEY_PRIVATE_SESSION = "private_session_enabled"
private const val KEY_AUTOPLAY = "autoplay_enabled"
private const val KEY_VOLUME_NORMALIZATION = "volume_normalization_enabled"
private const val KEY_OFFLINE_MODE = "offline_mode_enabled"
private const val KEY_DOWNLOAD_QUALITY = "download_quality"
private const val KEY_WIFI_AUTO_CACHE = "wifi_auto_cache_enabled"

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

    /** When true, only plays downloaded tracks and disables streaming. */
    var isOfflineModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_OFFLINE_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_OFFLINE_MODE, value).apply()

    /** When true, recently played and liked tracks are downloaded automatically on WiFi. */
    var isWifiAutoCacheEnabled: Boolean
        get() = prefs.getBoolean(KEY_WIFI_AUTO_CACHE, false)
        set(value) = prefs.edit().putBoolean(KEY_WIFI_AUTO_CACHE, value).apply()

    /** Default quality for new downloads. */
    var downloadQuality: DownloadQuality
        get() {
            val kbps = prefs.getInt(KEY_DOWNLOAD_QUALITY, DownloadQuality.HIGH.kbps)
            return DownloadQuality.entries.find { it.kbps == kbps } ?: DownloadQuality.HIGH
        }
        set(value) = prefs.edit().putInt(KEY_DOWNLOAD_QUALITY, value.kbps).apply()
}
