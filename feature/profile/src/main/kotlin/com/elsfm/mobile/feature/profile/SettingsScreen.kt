package com.elsfm.mobile.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elsfm.mobile.core.media.ShakeSensitivity
import com.elsfm.mobile.feature.profile.storage.StorageSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

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
