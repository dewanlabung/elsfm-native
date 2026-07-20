package com.elsfm.mobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var deepLink by mutableStateOf<DeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        deepLink = parseDeepLink(intent?.data)
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
    }

    // launchMode="singleTask" routes subsequent deep links here instead of a new onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink = parseDeepLink(intent.data)
    }

    private fun parseDeepLink(data: Uri?): DeepLink? {
        val segments = data?.pathSegments ?: return null
        return when (segments.getOrNull(0)) {
            "track" -> segments.getOrNull(1)?.toIntOrNull()?.let { DeepLink.Track(it) }
            "album" -> segments.getOrNull(1)?.toIntOrNull()?.let { DeepLink.Album(it) }
            "artist" -> segments.getOrNull(1)?.toIntOrNull()?.let { DeepLink.Artist(it) }
            "playlist" -> segments.getOrNull(1)?.toIntOrNull()?.let { DeepLink.Playlist(it) }
            else -> null
        }
    }
}
