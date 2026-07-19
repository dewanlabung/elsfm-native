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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var deepLinkTrackId by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep content below the status/navigation bars (don't go edge-to-edge).
        WindowCompat.setDecorFitsSystemWindows(window, true)
        deepLinkTrackId = extractTrackId(intent?.data)
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()

            ElsfmTheme(useDarkTheme = isDarkMode) {
                ElsfmNavHost(
                    deepLinkTrackId = deepLinkTrackId,
                    onDeepLinkConsumed = { deepLinkTrackId = null },
                )
            }
        }
    }

    // launchMode="singleTask" routes a second deep-link tap here instead of a new onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkTrackId = extractTrackId(intent.data)
    }

    /** Matches the real web track URL `/track/{id}/{slug}` (see AndroidManifest's intent filter). */
    private fun extractTrackId(data: Uri?): Int? {
        val segments = data?.pathSegments ?: return null
        if (segments.getOrNull(0) != "track") return null
        return segments.getOrNull(1)?.toIntOrNull()
    }
}
