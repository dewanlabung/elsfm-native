package com.elsfm.mobile.feature.discovery

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.feature.discovery.sections.FeaturedPlaylistsSection
import com.elsfm.mobile.feature.discovery.sections.NewReleasesSection
import com.elsfm.mobile.feature.discovery.sections.TrackListSection
import com.elsfm.mobile.feature.library.composables.SectionHeader

private const val CROSSFADE_DURATION_MS = 300
internal const val DISCOVERY_CONTENT_LIST_TEST_TAG = "discoveryContentList"

@Composable
fun DiscoveryScreen(
    onTrackClicked: (Track, List<Track>) -> Unit,
    viewModel: DiscoveryViewModel = hiltViewModel(),
    onSeeAllKidsZone: () -> Unit = {},
    onSeeAllExploreMoreChannel: () -> Unit = {},
    onSeeAllNewReleases: () -> Unit = {},
    onSeeAllMostlyPlayedSongs: () -> Unit = {},
    onPlaylistClicked: (Playlist) -> Unit = {},
    onAlbumClicked: (Album) -> Unit = {},
    onTrackMoreClicked: (Track) -> Unit = {},
    onChannelClicked: (channelId: Int) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Crossfade(
            targetState = state.isLoading,
            animationSpec = tween(durationMillis = CROSSFADE_DURATION_MS),
            label = "discoveryLoadingCrossfade",
        ) { isLoading ->
            when {
                isLoading -> DiscoveryLoading()
                state.isEmpty() && state.error != null -> DiscoveryError(state.error)
                else -> DiscoveryContent(
                    state = state,
                    onTrackClicked = onTrackClicked,
                    onSeeAllKidsZone = onSeeAllKidsZone,
                    onSeeAllExploreMoreChannel = onSeeAllExploreMoreChannel,
                    onSeeAllNewReleases = onSeeAllNewReleases,
                    onSeeAllMostlyPlayedSongs = onSeeAllMostlyPlayedSongs,
                    onPlaylistClicked = onPlaylistClicked,
                    onAlbumClicked = onAlbumClicked,
                    onTrackMoreClicked = onTrackMoreClicked,
                    onChannelClicked = onChannelClicked,
                )
            }
        }
    }
}

private fun DiscoveryUiState.isEmpty(): Boolean {
    return kidsZone.isEmpty() &&
        exploreMoreChannel.isEmpty() &&
        newReleases.isEmpty() &&
        mostlyPlayedSongs.isEmpty() &&
        recentlyPlayed.isEmpty()
}

@Composable
private fun DiscoveryLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun DiscoveryError(error: String?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Failed to load home",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
internal fun DiscoveryContent(
    state: DiscoveryUiState,
    onTrackClicked: (Track, List<Track>) -> Unit,
    onSeeAllKidsZone: () -> Unit,
    onSeeAllExploreMoreChannel: () -> Unit,
    onSeeAllNewReleases: () -> Unit,
    onSeeAllMostlyPlayedSongs: () -> Unit,
    onPlaylistClicked: (Playlist) -> Unit,
    onAlbumClicked: (Album) -> Unit,
    onTrackMoreClicked: (Track) -> Unit,
    onChannelClicked: (channelId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 16.dp)
            .testTag(DISCOVERY_CONTENT_LIST_TEST_TAG),
    ) {
        if (state.kidsZone.isNotEmpty()) {
            item {
                SectionHeader(title = state.kidsZoneTitle, onSeeAllClick = onSeeAllKidsZone)
                FeaturedPlaylistsSection(
                    playlists = state.kidsZone,
                    onPlaylistClick = onPlaylistClicked,
                )
            }
        }
        if (state.exploreMoreChannel.isNotEmpty()) {
            item {
                SectionHeader(
                    title = state.exploreMoreChannelTitle,
                    onSeeAllClick = {
                        state.exploreMoreChannelId?.let(onChannelClicked)
                        onSeeAllExploreMoreChannel()
                    },
                )
                FeaturedPlaylistsSection(
                    playlists = state.exploreMoreChannel,
                    onPlaylistClick = onPlaylistClicked,
                )
            }
        }
        if (state.newReleases.isNotEmpty()) {
            item {
                SectionHeader(title = state.newReleasesTitle, onSeeAllClick = onSeeAllNewReleases)
                NewReleasesSection(
                    albums = state.newReleases,
                    onAlbumClick = onAlbumClicked,
                )
            }
        }
        if (state.mostlyPlayedSongs.isNotEmpty()) {
            item {
                SectionHeader(title = state.mostlyPlayedSongsTitle, onSeeAllClick = onSeeAllMostlyPlayedSongs)
                TrackListSection(
                    tracks = state.mostlyPlayedSongs,
                    onTrackClick = { track -> onTrackClicked(track, state.mostlyPlayedSongs) },
                    onTrackMoreClick = onTrackMoreClicked,
                )
            }
        }
        if (state.error != null) {
            item {
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}
