package com.elsfm.mobile.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.elsfm.mobile.core.model.Track
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onCollapse: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsState()
    val menuState by viewModel.menuState.collectAsState()
    var menuAnchorX by remember { mutableFloatStateOf(0f) }
    var menuAnchorY by remember { mutableFloatStateOf(0f) }
    var isQueueVisible by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Blurred album art background with gradient overlay
        if (state.currentTrack != null) {
            AsyncImage(
                model = state.currentTrack?.image,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 32.dp),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.7f),
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            )
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Collapse button header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { onCollapse?.invoke() },
                    modifier = Modifier.testTag("player_collapse")
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Collapse player",
                        tint = Color.White,
                    )
                }

                IconButton(
                    onClick = { isQueueVisible = true },
                    modifier = Modifier.testTag("player_queue_toggle"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Show queue",
                        tint = Color.White,
                    )
                }
            }

            // Album artwork
            AnimatedVisibility(
                visible = state.currentTrack != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .testTag("player_album_art"),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        AsyncImage(
                            model = state.currentTrack?.image,
                            contentDescription = state.currentTrack?.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }

            // Track info with long-press menu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .pointerInput(state.currentTrack) {
                        detectTapGestures(
                            onLongPress = { offset ->
                                state.currentTrack?.let {
                                    menuAnchorX = offset.x
                                    menuAnchorY = offset.y
                                    viewModel.onMenuEvent(PlayerMenuEvent.ShowMenu(it.id))
                                }
                            }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.currentTrack?.name ?: "Nothing playing",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("player_track_title"),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.currentTrack?.artists?.firstOrNull()?.name ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("player_artist_name"),
                    )
                }

                // Track context menu
                TrackContextMenu(
                    trackId = state.currentTrack?.id ?: -1,
                    isVisible = menuState.isMenuVisible,
                    onDismiss = { viewModel.onMenuEvent(PlayerMenuEvent.HideMenu) },
                    onAddToPlaylist = { trackId ->
                        // TODO: Show playlist selection dialog, then call:
                        // viewModel.onMenuEvent(PlayerMenuEvent.AddToPlaylist(trackId, selectedPlaylistId))
                    },
                    onShare = {
                        state.currentTrack?.let {
                            viewModel.onMenuEvent(PlayerMenuEvent.ShareTrack(it.id))
                        }
                    }
                )
            }

            // Progress bar
            if (state.currentTrack != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    var sliderValue by remember { mutableFloatStateOf(0f) }
                    val currentMs = state.positionMs.toFloat()
                    val durationMs = (state.currentTrack?.durationMs ?: 1).toFloat()
                    val progress = if (durationMs > 0) currentMs / durationMs else 0f

                    Slider(
                        value = if (sliderValue >= 0) sliderValue else progress,
                        onValueChange = { newValue ->
                            sliderValue = newValue
                            viewModel.seekTo((newValue * durationMs).toLong())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_progress_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                        ),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = formatDuration(currentMs.toLong()),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Text(
                            text = formatDuration(durationMs.toLong()),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // Playback controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Previous button
                IconButton(
                    onClick = viewModel::skipPrevious,
                    modifier = Modifier.testTag("player_previous"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous track",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(modifier = Modifier.size(24.dp))

                // Play/Pause button with smooth scale animation
                val playPauseScale by animateFloatAsState(
                    targetValue = if (state.isPlaying) 1f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f),
                )
                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(playPauseScale)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    IconButton(
                        onClick = viewModel::togglePlayPause,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("player_play_pause"),
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.size(24.dp))

                // Next button
                IconButton(
                    onClick = viewModel::skipNext,
                    modifier = Modifier.testTag("player_next"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next track",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }

    if (isQueueVisible) {
        QueueBottomSheet(
            queue = state.queue,
            currentTrack = state.currentTrack,
            onTrackTap = { track ->
                viewModel.jumpToQueueItem(track)
                isQueueVisible = false
            },
            onDismiss = { isQueueVisible = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueBottomSheet(
    queue: List<Track>,
    currentTrack: Track?,
    onTrackTap: (Track) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag("player_queue_sheet"),
    ) {
        Text(
            text = "Up Next",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (queue.isEmpty()) {
            Text(
                text = "Queue is empty",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.testTag("player_queue_list")) {
                items(queue, key = { it.id }) { track ->
                    QueueRow(
                        track = track,
                        isCurrent = track.id == currentTrack?.id,
                        onClick = { onTrackTap(track) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            AsyncImage(
                model = track.image,
                contentDescription = track.name,
                modifier = Modifier.size(40.dp),
                contentScale = ContentScale.Crop,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artists.firstOrNull()?.name ?: "Unknown",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun TrackContextMenu(
    trackId: Int,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onAddToPlaylist: (Int) -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismiss,
            modifier = modifier
        ) {
            DropdownMenuItem(
                text = { Text("Add to Playlist") },
                onClick = { onAddToPlaylist(trackId); onDismiss() }
            )
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = { onShare(); onDismiss() }
            )
            DropdownMenuItem(
                text = { Text("View Details") },
                onClick = { onDismiss() }
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = max(0, ms / 1000)
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}
