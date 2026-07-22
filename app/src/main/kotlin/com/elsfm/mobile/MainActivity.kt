package com.elsfm.mobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.elsfm.mobile.core.designsystem.ElsfmTheme
import com.elsfm.mobile.feature.profile.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

sealed class DeepLink {
    data class Track(val trackId: Int) : DeepLink()
    data class Album(val albumId: Int) : DeepLink()
    data class Artist(val artistId: Int) : DeepLink()
    data class Playlist(val playlistId: Int) : DeepLink()
    data class PasswordReset(val token: String, val email: String) : DeepLink()
    data object Search : DeepLink()
    data object Library : DeepLink()
    data object Player : DeepLink()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var deepLink by mutableStateOf<DeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        deepLink = parseShortcutDestination(intent) ?: parseDeepLink(intent?.data)
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()

            ElsfmTheme(useDarkTheme = isDarkMode) {
                ElsfmNavHost(
                    deepLink = deepLink,
                    onDeepLinkConsumed = { deepLink = null },
                )
            }
        }
        requestBatteryOptimizationExemption()
    }

    // launchMode="singleTask" routes subsequent deep links here instead of a new onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink = parseShortcutDestination(intent) ?: parseDeepLink(intent.data)
    }

    // Feature 7: long-press launcher shortcuts route here via a "shortcut_destination" extra.
    private fun parseShortcutDestination(intent: Intent?): DeepLink? =
        when (intent?.getStringExtra("shortcut_destination")) {
            "search" -> DeepLink.Search
            "library" -> DeepLink.Library
            "player" -> DeepLink.Player
            else -> null
        }

    private fun parseDeepLink(data: Uri?): DeepLink? {
        val segments = data?.pathSegments ?: return null
        return when (segments.getOrNull(0)) {
            "track" -> segments.getOrNull(1)?.toIntOrNull()?.let { DeepLink.Track(it) }
            "album" -> segments.getOrNull(1)?.toIntOrNull()?.let { DeepLink.Album(it) }
            "artist" -> segments.getOrNull(1)?.toIntOrNull()?.let { DeepLink.Artist(it) }
            "playlist" -> segments.getOrNull(1)?.toIntOrNull()?.let { DeepLink.Playlist(it) }
            "password" -> {
                if (segments.getOrNull(1) == "reset") {
                    val token = segments.getOrNull(2) ?: return null
                    val email = data.getQueryParameter("email") ?: return null
                    DeepLink.PasswordReset(token, email)
                } else {
                    null
                }
            }
            else -> null
        }
    }

    // Feature 4: prompt once to whitelist ELSFM from OS battery killers (Samsung/Xiaomi/etc.).
    // Without exemption, aggressive OEM battery savers can kill background playback.
    private fun requestBatteryOptimizationExemption() {
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        val prefs = getSharedPreferences("elsfm_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("battery_opt_requested", false)) return
        prefs.edit().putBoolean("battery_opt_requested", true).apply()
        startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
        )
    }
}
