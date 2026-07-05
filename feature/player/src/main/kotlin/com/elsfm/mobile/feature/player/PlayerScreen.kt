package com.elsfm.mobile.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PlayerScreen(viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(state.currentTrack?.name ?: "Nothing playing", modifier = Modifier.testTag("player_track_title"))
        Text(state.currentTrack?.artists?.firstOrNull()?.name.orEmpty())
        Button(onClick = viewModel::skipPrevious, modifier = Modifier.testTag("player_previous")) { Text("Previous") }
        Button(onClick = viewModel::togglePlayPause, modifier = Modifier.testTag("player_play_pause")) {
            Text(if (state.isPlaying) "Pause" else "Play")
        }
        Button(onClick = viewModel::skipNext, modifier = Modifier.testTag("player_next")) { Text("Next") }
    }
}
