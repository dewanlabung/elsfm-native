package com.elsfm.mobile.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elsfm.mobile.core.model.Channel

@Composable
fun ChannelListComposable(
    channels: List<Channel>,
    onChannelClicked: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(channels, key = { it.id }) { channel ->
            Text(
                text = channel.name,
                modifier = Modifier
                    .clickable { onChannelClicked(channel) }
                    .padding(16.dp),
            )
        }
    }
}
