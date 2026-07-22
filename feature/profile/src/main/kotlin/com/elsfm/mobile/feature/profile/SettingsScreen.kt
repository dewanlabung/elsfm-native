package com.elsfm.mobile.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elsfm.mobile.core.media.ShakeSensitivity
import com.elsfm.mobile.feature.profile.storage.StorageSection

private val accentColorPresets = listOf(
    "#1B5E20" to "Forest",
    "#0D47A1" to "Ocean",
    "#4A148C" to "Plum",
    "#B71C1C" to "Ruby",
    "#E65100" to "Ember",
    "#37474F" to "Slate",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val accentColorHex by themeViewModel.accentColorHex.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Playback ──────────────────────────────────────────────────
            SettingsSectionHeader("Playback")

            ListItem(
                headlineContent = { Text("Shake to skip") },
                supportingContent = { Text("Shake the device to jump to the next track") },
                trailingContent = {
                    Switch(
                        checked = state.shakeEnabled,
                        onCheckedChange = { viewModel.toggleShake() },
                    )
                },
            )

            if (state.shakeEnabled) {
                ListItem(
                    headlineContent = { Text("Shake sensitivity") },
                    supportingContent = {
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            ShakeSensitivity.entries.forEach { level ->
                                FilterChip(
                                    selected = state.shakeSensitivity == level,
                                    onClick = { viewModel.setSensitivity(level) },
                                    label = {
                                        Text(
                                            text = level.name.lowercase()
                                                .replaceFirstChar { it.uppercaseChar() },
                                        )
                                    },
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }
                        }
                    },
                )
            }

            ListItem(
                headlineContent = { Text("Autoplay") },
                supportingContent = { Text("Continue playing similar songs when the queue ends") },
                trailingContent = {
                    Switch(
                        checked = state.isAutoplayEnabled,
                        onCheckedChange = { viewModel.toggleAutoplay() },
                    )
                },
            )

            ListItem(
                headlineContent = { Text("Volume normalization") },
                supportingContent = { Text("Equalize volume levels across tracks") },
                trailingContent = {
                    Switch(
                        checked = state.isVolumeNormalizationEnabled,
                        onCheckedChange = { viewModel.toggleVolumeNormalization() },
                    )
                },
            )

            ListItem(
                headlineContent = { Text("Skip silence") },
                supportingContent = { Text("Automatically skip silent segments within tracks") },
                trailingContent = {
                    Switch(
                        checked = state.isSkipSilenceEnabled,
                        onCheckedChange = { viewModel.toggleSkipSilence() },
                    )
                },
            )

            ListItem(
                headlineContent = { Text("Persistent queue") },
                supportingContent = { Text("Save the playback queue and restore it when you reopen the app") },
                trailingContent = {
                    Switch(
                        checked = state.isPersistentQueueEnabled,
                        onCheckedChange = { viewModel.togglePersistentQueue() },
                    )
                },
            )

            if (state.isPersistentQueueEnabled) {
                ListItem(
                    headlineContent = { Text("Resume playback") },
                    supportingContent = { Text("Resume playing automatically when you reopen the app") },
                    trailingContent = {
                        Switch(
                            checked = state.isResumePlaybackEnabled,
                            onCheckedChange = { viewModel.toggleResumePlayback() },
                        )
                    },
                )
            }

            // ── Session ───────────────────────────────────────────────────
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsSectionHeader("Session")

            ListItem(
                headlineContent = { Text("Private session") },
                supportingContent = {
                    Text(
                        if (state.isPrivateSession) {
                            "Listening activity is not recorded"
                        } else {
                            "Your plays are added to listening history"
                        }
                    )
                },
                trailingContent = {
                    Switch(
                        checked = state.isPrivateSession,
                        onCheckedChange = { viewModel.togglePrivateSession() },
                    )
                },
            )

            StorageSection()

            // ── Downloads ─────────────────────────────────────────────────
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsSectionHeader("Downloads")

            ListItem(
                headlineContent = { Text("Offline mode") },
                supportingContent = { Text("Only play downloaded tracks") },
                trailingContent = {
                    Switch(
                        checked = state.isOfflineModeEnabled,
                        onCheckedChange = { viewModel.toggleOfflineMode() },
                    )
                },
            )

            ListItem(
                headlineContent = { Text("Auto-cache on WiFi") },
                supportingContent = { Text("Download recent and liked tracks automatically when on WiFi") },
                trailingContent = {
                    Switch(
                        checked = state.isWifiAutoCacheEnabled,
                        onCheckedChange = { viewModel.toggleWifiAutoCache() },
                    )
                },
            )

            // ── Appearance ────────────────────────────────────────────────
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsSectionHeader("Appearance")

            ListItem(
                headlineContent = { Text("Header color") },
                supportingContent = {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        accentColorPresets.forEach { (hex, label) ->
                            val isSelected = accentColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        try { Color(android.graphics.Color.parseColor(hex)) }
                                        catch (_: Exception) { Color.Gray }
                                    )
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { themeViewModel.setAccentColor(hex) },
                            )
                        }
                    }
                },
            )

            // ── Other ─────────────────────────────────────────────────────
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsSectionHeader("Other")

            ListItem(
                headlineContent = { Text("Keep screen on") },
                supportingContent = { Text("Prevent the screen from turning off while a track is playing") },
                trailingContent = {
                    Switch(
                        checked = state.isKeepScreenOnEnabled,
                        onCheckedChange = { viewModel.toggleKeepScreenOn() },
                    )
                },
            )

            ListItem(
                headlineContent = { Text("Pause search history") },
                supportingContent = { Text("Stop saving search queries to your history") },
                trailingContent = {
                    Switch(
                        checked = state.isSearchHistoryPaused,
                        onCheckedChange = { viewModel.toggleSearchHistoryPaused() },
                    )
                },
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
