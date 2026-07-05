package com.elsfm.mobile.feature.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.elsfm.mobile.core.model.Channel
import androidx.compose.ui.unit.dp

@Composable
fun FeaturedPlaylistsSection(
    channels: List<Channel>,
    onChannelClicked: (Channel) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        items(channels) { channel ->
            FeaturedPlaylistCard(
                channel = channel,
                onClicked = { onChannelClicked(channel) },
            )
        }
    }
}

@Composable
fun FeaturedPlaylistCard(
    channel: Channel,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClicked,
        modifier = modifier
            .width(160.dp)
            .clip(MaterialTheme.shapes.medium),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(Modifier.fillMaxSize())
                }
            }
            Column(Modifier.padding(8.dp)) {
                Text(
                    channel.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
