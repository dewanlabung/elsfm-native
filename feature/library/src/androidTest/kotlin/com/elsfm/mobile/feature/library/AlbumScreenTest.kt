package com.elsfm.mobile.feature.library

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Track
import org.junit.Rule
import org.junit.Test

class AlbumScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val album = Album(id = 1, name = "New Beginnings", image = null, releaseDate = "2026-06-15")
    private val tracks = listOf(
        Track(id = 1, name = "Track One", image = null, durationMs = 180000, artists = emptyList()),
        Track(id = 2, name = "Track Two", image = null, durationMs = 200000, artists = emptyList()),
    )

    private fun setContent(
        state: AlbumDetailState,
        onPlayAll: () -> Unit = {},
        onTrackTap: (Track, List<Track>) -> Unit = { _, _ -> },
        onAddToQueue: (Track) -> Unit = {},
    ) {
        composeRule.setContent {
            AlbumDetailContent(
                state = state,
                onPlayAll = onPlayAll,
                onTrackTap = onTrackTap,
                onAddToQueue = onAddToQueue,
            )
        }
    }

    @Test
    fun rendersAlbumHeaderAndTracks() {
        setContent(AlbumDetailState(album = album, tracks = tracks))

        composeRule.onNodeWithText("New Beginnings").assertExists()
        composeRule.onNodeWithText("2026-06-15").assertExists()
        composeRule.onNodeWithText("2 tracks").assertExists()
        composeRule.onNodeWithText("Track One").assertExists()
        composeRule.onNodeWithText("Play All").assertExists()
    }

    @Test
    fun tappingPlayAllInvokesCallback() {
        var invoked = false
        setContent(AlbumDetailState(album = album, tracks = tracks), onPlayAll = { invoked = true })

        composeRule.onNodeWithText("Play All").performClick()

        assert(invoked) { "onPlayAll was not invoked" }
    }

    @Test
    fun tappingTrackInvokesOnTrackTap() {
        var tappedTrack: Track? = null
        setContent(
            AlbumDetailState(album = album, tracks = tracks),
            onTrackTap = { track, _ -> tappedTrack = track },
        )

        composeRule.onNodeWithText("Track One").performClick()

        assert(tappedTrack == tracks.first()) { "onTrackTap was not invoked with the correct track" }
    }

    @Test
    fun rendersErrorMessageWhenAlbumIsNull() {
        setContent(AlbumDetailState(album = null, error = "Failed to load album tracks"))

        composeRule.onNodeWithText("Failed to load album tracks").assertExists()
    }
}
