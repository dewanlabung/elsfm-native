package com.elsfm.mobile.feature.artist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.elsfm.mobile.core.designsystem.TrackContextMenu
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.ArtistFollower
import com.elsfm.mobile.core.model.ArtistLink
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.feature.library.composables.AlbumCard
import com.elsfm.mobile.feature.library.composables.SectionHeader
import com.elsfm.mobile.feature.library.composables.TrackListItem

@Composable
fun ArtistDetailScreen(
    artistId: Int,
    onTrackClicked: (track: Track, queue: List<Track>) -> Unit,
    onAlbumClicked: (albumId: Int) -> Unit,
    onArtistClicked: (artistId: Int) -> Unit = {},
    onUserClicked: (userId: Int) -> Unit = {},
    viewModel: ArtistDetailViewModel = hiltViewModel(key = artistId.toString()),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                ArtistErrorState(
                    message = state.error ?: "Unknown error",
                    onRetry = { viewModel.retryLoadArtistDetails() },
                )
            }
            else -> state.artist?.let { artist ->
                Column(modifier = Modifier.fillMaxSize()) {
                    ArtistHeader(
                        artist = artist,
                        followedByUser = state.followedByUser,
                        isFollowLoading = state.isFollowLoading,
                        onToggleFollow = { viewModel.toggleFollow() },
                        onPlay = {
                            state.tracks.firstOrNull()?.let { onTrackClicked(it, state.tracks) }
                        },
                        onCopyLink = {
                            viewModel.buildArtistShareUrl()?.let { url -> copyToClipboard(context, url) }
                        },
                        onShare = {
                            viewModel.buildArtistShareUrl()?.let { url -> shareUrl(context, artist.name, url) }
                        },
                    )

                    ArtistTabRow(
                        selectedTab = state.selectedTab,
                        onTabSelected = { viewModel.selectTab(it) },
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (state.selectedTab) {
                            ArtistTab.DISCOGRAPHY -> DiscographyTab(
                                tracks = state.tracks,
                                onTrackClicked = { track -> onTrackClicked(track, state.tracks) },
                            )
                            ArtistTab.SIMILAR_ARTISTS -> SimilarArtistsTab(
                                similarArtists = artist.similar,
                                onArtistClicked = onArtistClicked,
                            )
                            ArtistTab.ABOUT -> AboutTab(artist = artist)
                            ArtistTab.TRACKS -> TracksTab(
                                tracks = state.tracks,
                                onTrackClicked = { track -> onTrackClicked(track, state.tracks) },
                            )
                            ArtistTab.ALBUMS -> AlbumsTab(
                                albums = state.albums,
                                onAlbumClicked = onAlbumClicked,
                            )
                            ArtistTab.FOLLOWERS -> FollowersTab(
                                followers = state.followers,
                                isLoading = state.isFollowersLoading,
                                error = state.followersError,
                                followedUserIds = state.followedUserIds,
                                onToggleFollowUser = { viewModel.toggleFollowUser(it) },
                                onUserClicked = onUserClicked,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Error Loading Artist",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun ArtistHeader(
    artist: Artist,
    followedByUser: Boolean,
    isFollowLoading: Boolean,
    onToggleFollow: () -> Unit,
    onPlay: () -> Unit,
    onCopyLink: () -> Unit,
    onShare: () -> Unit,
) {
    var isMoreMenuVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Top: avatar left, info right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            AsyncImage(
                model = artist.image,
                contentDescription = artist.name,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (artist.verified) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Verified artist",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1DB954),
                        )
                    }
                }

                val location = listOfNotNull(artist.profile?.city, artist.profile?.country)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                if (location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    )
                }

                val bio = artist.profile?.description
                if (!bio.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Social links — one colored circle per platform, no duplicate share icons
                if (artist.links.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        artist.links.forEach { link ->
                            SocialLinkBadge(link = link, context = context)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action row: [▶ Play] [♥ Follow/Following] [More ▾]  ·  ▶ plays  ♥ likes
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("artist_play_button"),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play", style = MaterialTheme.typography.labelMedium)
                }

                if (followedByUser) {
                    Button(
                        onClick = onToggleFollow,
                        enabled = !isFollowLoading,
                        modifier = Modifier.height(38.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1DB954),
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                    ) {
                        Icon(imageVector = Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Following", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    OutlinedButton(
                        onClick = onToggleFollow,
                        enabled = !isFollowLoading,
                        modifier = Modifier.height(38.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                    ) {
                        Icon(imageVector = Icons.Filled.FavoriteBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Follow", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Box {
                    OutlinedButton(
                        onClick = { isMoreMenuVisible = true },
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("artist_more_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Text("More", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = isMoreMenuVisible,
                        onDismissRequest = { isMoreMenuVisible = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Copy artist link") },
                            onClick = { onCopyLink(); isMoreMenuVisible = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = { onShare(); isMoreMenuVisible = false },
                        )
                    }
                }
            }

            // Plays + likes stats
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val plays = artist.plays
                if (!plays.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        )
                        Text(
                            text = plays,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        )
                    }
                }
                if ((artist.likesCount ?: 0) > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = (artist.likesCount ?: 0).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialLinkBadge(link: ArtistLink, context: Context) {
    val url = link.url.lowercase()
    val (label, bgColor) = when {
        "facebook" in url -> "f" to Color(0xFF1877F2)
        "youtube" in url -> "▶" to Color(0xFFFF0000)
        "instagram" in url -> "ig" to Color(0xFFE1306C)
        "twitter" in url || "x.com" in url -> "X" to Color(0xFF14171A)
        "spotify" in url -> "S" to Color(0xFF1DB954)
        else -> "↗" to Color(0xFF607D8B)
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ArtistTabRow(
    selectedTab: ArtistTab,
    onTabSelected: (ArtistTab) -> Unit,
) {
    val tabs = listOf(
        ArtistTab.DISCOGRAPHY to "Discography",
        ArtistTab.SIMILAR_ARTISTS to "Similar Artists",
        ArtistTab.ABOUT to "About",
        ArtistTab.TRACKS to "Tracks",
        ArtistTab.ALBUMS to "Albums",
        ArtistTab.FOLLOWERS to "Followers",
    )
    ScrollableTabRow(
        selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0),
        modifier = Modifier.testTag("artist_tab_row"),
        edgePadding = 0.dp,
    ) {
        tabs.forEach { (tab, label) ->
            Tab(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                text = { Text(label, maxLines = 1) },
                modifier = Modifier.testTag("artist_tab_${tab.name}"),
            )
        }
    }
}

@Composable
private fun DiscographyTab(
    tracks: List<Track>,
    onTrackClicked: (Track) -> Unit,
) {
    var showAll by remember { mutableStateOf(false) }
    val displayedTracks = if (showAll) tracks else tracks.take(5)

    if (tracks.isEmpty()) {
        EmptyTabMessage("No tracks available")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            Text(
                text = "Popular songs",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        itemsIndexed(displayedTracks, key = { _, t -> t.id }) { index, track ->
            PopularTrackRow(
                index = index + 1,
                track = track,
                onClick = { onTrackClicked(track) },
            )
        }
        if (tracks.size > 5) {
            item {
                TextButton(
                    onClick = { showAll = !showAll },
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(if (showAll) "Show less" else "Show more")
                }
            }
        }
    }
}

@Composable
private fun PopularTrackRow(index: Int, track: Track, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.width(20.dp),
        )
        AsyncImage(
            model = track.image,
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val artistNames = track.artists.joinToString(", ") { it.name }
            Text(
                text = artistNames,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Filled.FavoriteBorder,
            contentDescription = "Like",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
        val minutes = track.durationMs / 60_000
        val seconds = (track.durationMs % 60_000) / 1_000
        Text(
            text = "%d:%02d".format(minutes, seconds),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun AlbumsTab(
    albums: List<com.elsfm.mobile.core.model.Album>,
    onAlbumClicked: (Int) -> Unit,
) {
    if (albums.isEmpty()) {
        EmptyTabMessage("No albums available")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val chunked = albums.chunked(2)
        items(chunked) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { album ->
                    AlbumCard(
                        album = album,
                        onClick = { onAlbumClicked(album.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SimilarArtistsTab(
    similarArtists: List<Artist>,
    onArtistClicked: (Int) -> Unit,
) {
    if (similarArtists.isEmpty()) {
        EmptyTabMessage("No similar artists available")
        return
    }
    LazyRow(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(similarArtists, key = { it.id }) { similar ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onArtistClicked(similar.id) },
            ) {
                AsyncImage(
                    model = similar.image,
                    contentDescription = similar.name,
                    modifier = Modifier.size(80.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = similar.name, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AboutTab(artist: Artist) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            val bio = artist.profile?.description
            if (bio.isNullOrBlank()) {
                Text(text = "No bio available", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(text = bio, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TracksTab(
    tracks: List<Track>,
    onTrackClicked: (Track) -> Unit,
    onTrackMoreClicked: (Track) -> Unit = {},
) {
    if (tracks.isEmpty()) {
        EmptyTabMessage("No tracks available")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("artist_tracks_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        items(tracks, key = { it.id }) { track ->
            var menuExpanded by remember { mutableStateOf(false) }
            Box {
                TrackListItem(
                    track = track,
                    isPlaying = false,
                    onClick = { onTrackClicked(track) },
                    onMoreClick = { menuExpanded = true },
                )
                TrackContextMenu(
                    trackId = track.id,
                    artistId = track.artists.firstOrNull()?.id,
                    albumId = track.album?.id,
                    isVisible = menuExpanded,
                    onDismiss = { menuExpanded = false },
                    onAddToQueue = { onTrackMoreClicked(track) },
                    onAddToLibrary = {},
                    onAddToPlaylist = {},
                    onShare = {},
                    onRepost = {},
                )
            }
        }
    }
}

@Composable
private fun FollowersTab(
    followers: List<ArtistFollower>,
    isLoading: Boolean,
    error: String?,
    followedUserIds: Set<Int>,
    onToggleFollowUser: (Int) -> Unit,
    onUserClicked: (Int) -> Unit = {},
) {
    when {
        isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        error != null -> EmptyTabMessage(error)
        followers.isEmpty() -> EmptyTabMessage("No followers yet")
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("artist_followers_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            items(followers, key = { it.id }) { follower ->
                FollowerRow(
                    follower = follower,
                    isFollowed = followedUserIds.contains(follower.id),
                    onToggleFollow = { onToggleFollowUser(follower.id) },
                    onClick = { onUserClicked(follower.id) },
                )
            }
        }
    }
}

@Composable
private fun FollowerRow(
    follower: ArtistFollower,
    isFollowed: Boolean,
    onToggleFollow: () -> Unit,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = follower.image,
            contentDescription = follower.name,
            modifier = Modifier.size(44.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = follower.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = onToggleFollow) {
            Text(if (isFollowed) "Following" else "Follow")
        }
    }
}

@Composable
private fun EmptyTabMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Artist link", text))
}

private fun shareUrl(context: Context, artistName: String, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
        putExtra(Intent.EXTRA_SUBJECT, artistName)
    }
    context.startActivity(Intent.createChooser(intent, "Share artist"))
}
