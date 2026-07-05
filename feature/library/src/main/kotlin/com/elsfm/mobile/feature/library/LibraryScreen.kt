package com.elsfm.mobile.feature.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LibraryScreen(
    onChannelSelected: (channelId: Int) -> Unit,
    libraryViewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by libraryViewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            state.error != null -> {
                Text(
                    text = state.error.orEmpty(),
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
            }
            else -> {
                ChannelListComposable(
                    channels = state.channels,
                    onChannelClicked = { channel ->
                        libraryViewModel.selectChannel(channel.id)
                        onChannelSelected(channel.id)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
