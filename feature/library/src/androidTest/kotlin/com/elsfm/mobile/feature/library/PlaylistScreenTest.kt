package com.elsfm.mobile.feature.library

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.Track
import org.junit.Rule
import org.junit.Test

class PlaylistScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val playlist = Playlist(id = 1, name = "Sunday Worship Favorites", image = null)
    private val tracks = listOf(
        Track(id = 1, name = "Track One", image = null, durationMs = 180000, artists = emptyList()),
        Track(id = 2, name = "Track Two", image = null, durationMs = 200000, artists = emptyList()),
    )

    private fun setContent(
        state: PlaylistDetailState,
        onPlayAll: () -> Unit = {},
        onTrackTap: (Track, List<Track>) -> Unit = { _, _ -> },
        onAddToQueue: (Track) -> Unit = {},
        onDeleteTrack: (Int) -> Unit = {},
        onLoadNextPage: () -> Unit = {},
    ) {
        composeRule.setContent {
            PlaylistDetailContent(
                state = state,
                onPlayAll = onPlayAll,
                onTrackTap = onTrackTap,
                onAddToQueue = onAddToQueue,
                onDeleteTrack = onDeleteTrack,
                onLoadNextPage = onLoadNextPage,
            )
        }
    }

    @Test
    fun rendersPlaylistHeaderAndTracks() {
        setContent(PlaylistDetailState(playlist = playlist, tracks = tracks))

        composeRule.onNodeWithText("Sunday Worship Favorites").assertExists()
        composeRule.onNodeWithText("2 tracks").assertExists()
        composeRule.onNodeWithText("Track One").assertExists()
        composeRule.onNodeWithText("Track Two").assertExists()
        composeRule.onNodeWithText("Play All").assertExists()
    }

    @Test
    fun tappingPlayAllInvokesCallback() {
        var invoked = false
        setContent(PlaylistDetailState(playlist = playlist, tracks = tracks), onPlayAll = { invoked = true })

        composeRule.onNodeWithText("Play All").performClick()

        assert(invoked) { "onPlayAll was not invoked" }
    }

    @Test
    fun tappingTrackInvokesOnTrackTap() {
        var tappedTrack: Track? = null
        setContent(
            PlaylistDetailState(playlist = playlist, tracks = tracks),
            onTrackTap = { track, _ -> tappedTrack = track },
        )

        composeRule.onNodeWithText("Track One").performClick()

        assert(tappedTrack == tracks.first()) { "onTrackTap was not invoked with the correct track" }
    }

    @Test
    fun rendersErrorMessageWhenPlaylistIsNull() {
        setContent(PlaylistDetailState(playlist = null, error = "Failed to load playlist tracks"))

        composeRule.onNodeWithText("Failed to load playlist tracks").assertExists()
    }

    @Test
    fun scrollingNearTheEndInvokesOnLoadNextPage() {
        val manyTracks = (1..30).map {
            Track(id = it, name = "Track $it", image = null, durationMs = 180000, artists = emptyList())
        }
        var invoked = false
        setContent(
            PlaylistDetailState(playlist = playlist, tracks = manyTracks, hasMoreTracks = true),
            onLoadNextPage = { invoked = true },
        )

        composeRule
            .onNodeWithTag(PLAYLIST_LAZY_COLUMN_TEST_TAG)
            .performScrollToIndex(manyTracks.lastIndex)

        composeRule.waitUntil(timeoutMillis = 5_000) { invoked }
    }

    @Test
    fun loadingMoreIndicatorShownOnlyWhileFetchingNextPage() {
        setContent(PlaylistDetailState(playlist = playlist, tracks = tracks, isLoadingMore = true))

        composeRule.onNodeWithTag(PLAYLIST_LOAD_MORE_INDICATOR_TEST_TAG).assertExists()
    }

    @Test
    fun loadingMoreIndicatorHiddenWhenNotFetchingNextPage() {
        setContent(PlaylistDetailState(playlist = playlist, tracks = tracks, isLoadingMore = false))

        composeRule.onAllNodesWithTag(PLAYLIST_LOAD_MORE_INDICATOR_TEST_TAG).assertCountEquals(0)
    }
}
