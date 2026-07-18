package com.elsfm.mobile.feature.profile.storage

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun StorageSection(viewModel: StorageViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    SettingsSectionHeader("Storage")

    ListItem(
        headlineContent = { Text("Cached data") },
        supportingContent = { Text(formatBytes(state.totalCacheBytes)) },
        trailingContent = {
            Icon(
                if (state.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
            )
        },
        modifier = Modifier.clickable { viewModel.toggleExpanded() },
    )

    if (state.isExpanded) {
        state.entries.forEach { entry ->
            ListItem(
                headlineContent = { Text(entry.label) },
                trailingContent = { Text(formatBytes(entry.sizeBytes)) },
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        ListItem(
            headlineContent = { Text("Downloaded tracks") },
            supportingContent = { Text("Manage in Downloads") },
            trailingContent = { Text(formatBytes(state.downloadedTracksBytes)) },
            modifier = Modifier.padding(start = 16.dp),
        )
        TextButton(
            onClick = { viewModel.clearAllCaches() },
            enabled = !state.isClearing && state.totalCacheBytes > 0,
            modifier = Modifier.padding(start = 16.dp),
        ) {
            Text(if (state.isClearing) "Clearing…" else "Clear cache")
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024f)
    else -> "%.1f MB".format(bytes / (1024f * 1024f))
}
