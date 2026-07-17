package com.elsfm.mobile.feature.artist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.ArtistFollower
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
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
                                albums = state.albums,
                                onAlbumClicked = onAlbumClicked,
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error Loading Artist",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistHeader(
    artist: Artist,
    followedByUser: Boolean,
    onToggleFollow: () -> Unit,
    onPlay: () -> Unit,
    onCopyLink: () -> Unit,
    onShare: () -> Unit,
) {
    var isMoreMenuVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = artist.image,
            contentDescription = artist.name,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (artist.verified) {
                Spacer(modifier = Modifier.height(0.dp).size(4.dp))
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Verified artist",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp).padding(start = 4.dp),
                )
            }
        }

        val location = listOfNotNull(artist.profile?.city, artist.profile?.country)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        if (location.isNotBlank()) {
            Text(
                text = location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }

        if (artist.links.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                artist.links.forEach { link ->
                    // Material core icons have no brand-specific (Facebook/YouTube/Instagram/X)
                    // glyphs; material-icons-extended is not a project dependency, so a generic
                    // link icon is used for every social link instead of fabricating brand marks.
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = link.title,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = (artist.likesCount ?: 0).toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Likes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = (artist.followersCount ?: 0).toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Followers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onPlay,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("artist_play_button"),
            ) {
                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.size(4.dp))
                Text("Play")
            }

            Box {
                OutlinedButton(
                    onClick = { isMoreMenuVisible = true },
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("artist_more_button"),
                ) {
                    Text("More")
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More options")
                }

                DropdownMenu(
                    expanded = isMoreMenuVisible,
                    onDismissRequest = { isMoreMenuVisible = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(if (followedByUser) "Following" else "Follow") },
                        onClick = { onToggleFollow(); isMoreMenuVisible = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy artist link") },
                        onClick = { onCopyLink(); isMoreMenuVisible = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = { onShare(); isMoreMenuVisible = false }
                    )
                }
            }
        }
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
        ArtistTab.FOLLOWERS to "Followers",
    )
    TabRow(
        selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0),
        modifier = Modifier.testTag("artist_tab_row"),
    ) {
        tabs.forEach { (tab, label) ->
            Tab(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                text = { Text(label) },
                modifier = Modifier.testTag("artist_tab_${tab.name}"),
            )
        }
    }
}

@Composable
private fun DiscographyTab(
    albums: List<com.elsfm.mobile.core.model.Album>,
    onAlbumClicked: (Int) -> Unit,
) {
    if (albums.isEmpty()) {
        EmptyTabMessage("No albums available")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item { SectionHeader(title = "Popular songs") }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                albums.take(2).forEach { album ->
                    AlbumCard(
                        album = album,
                        onClick = { onAlbumClicked(album.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = similar.name,
                    style = MaterialTheme.typography.bodySmall,
                )
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
                Text(
                    text = "No bio available",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun TracksTab(
    tracks: List<Track>,
    onTrackClicked: (Track) -> Unit,
) {
    if (tracks.isEmpty()) {
        EmptyTabMessage("No tracks available")
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("artist_tracks_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        items(tracks, key = { it.id }) { track ->
            TrackListItem(
                track = track,
                isPlaying = false,
                onClick = { onTrackClicked(track) },
                onMoreClick = {},
            )
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
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        error != null -> EmptyTabMessage(error)
        followers.isEmpty() -> EmptyTabMessage("No followers yet")
        else -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("artist_followers_list"),
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
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
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
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium
        )
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
