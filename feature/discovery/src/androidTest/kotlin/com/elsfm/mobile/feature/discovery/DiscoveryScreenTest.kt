package com.elsfm.mobile.feature.discovery

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.Track
import org.junit.Rule
import org.junit.Test

class DiscoveryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun testState() = DiscoveryUiState(
        featured = listOf(
            Playlist(id = 8, name = "All Sunday School Songs", image = null),
        ),
        popular = listOf(
            Track(id = 1, name = "Popular Track", image = null, durationMs = 180000, artists = emptyList()),
        ),
        newReleases = listOf(
            Album(id = 460, name = "2026 EL Shaddai Youth Camp Songs", image = null, releaseDate = "2026-02-08"),
        ),
        recentlyPlayed = listOf(
            Track(id = 2, name = "Recent Track", image = null, durationMs = 200000, artists = emptyList()),
        ),
        isLoading = false,
        error = null,
    )

    private fun setContentWithData(
        onSeeAllFeatured: () -> Unit = {},
        onSeeAllPopular: () -> Unit = {},
        onSeeAllNewReleases: () -> Unit = {},
        onSeeAllRecentlyPlayed: () -> Unit = {},
    ) {
        composeRule.setContent {
            DiscoveryContent(
                state = testState(),
                onTrackClicked = { _, _ -> },
                onSeeAllFeatured = onSeeAllFeatured,
                onSeeAllPopular = onSeeAllPopular,
                onSeeAllNewReleases = onSeeAllNewReleases,
                onSeeAllRecentlyPlayed = onSeeAllRecentlyPlayed,
                onPlaylistClicked = {},
                onAlbumClicked = {},
                onTrackMoreClicked = {},
            )
        }
    }

    /** Scrolls the Discovery LazyColumn so section [index] is composed and reachable. */
    private fun scrollToSection(index: Int) {
        composeRule.onNodeWithTag(DISCOVERY_CONTENT_LIST_TEST_TAG).performScrollToIndex(index)
    }

    @Test
    fun rendersAllFourSectionTitles() {
        setContentWithData()

        composeRule.onNodeWithText("Featured Playlists").assertExists()
        scrollToSection(1)
        composeRule.onNodeWithText("Popular Songs").assertExists()
        scrollToSection(2)
        composeRule.onNodeWithText("New Releases").assertExists()
        scrollToSection(3)
        composeRule.onNodeWithText("Recently Played").assertExists()
    }

    @Test
    fun seeAllFeaturedInvokesCallback() {
        var invoked = false
        setContentWithData(onSeeAllFeatured = { invoked = true })

        composeRule.onNodeWithText("Featured Playlists").performClick()

        assert(invoked) { "onSeeAllFeatured was not invoked" }
    }

    @Test
    fun seeAllPopularInvokesCallback() {
        var invoked = false
        setContentWithData(onSeeAllPopular = { invoked = true })

        scrollToSection(1)
        composeRule.onNodeWithText("Popular Songs").performClick()

        assert(invoked) { "onSeeAllPopular was not invoked" }
    }

    @Test
    fun seeAllNewReleasesInvokesCallback() {
        var invoked = false
        setContentWithData(onSeeAllNewReleases = { invoked = true })

        scrollToSection(2)
        composeRule.onNodeWithText("New Releases").performClick()

        assert(invoked) { "onSeeAllNewReleases was not invoked" }
    }

    @Test
    fun seeAllRecentlyPlayedInvokesCallback() {
        var invoked = false
        setContentWithData(onSeeAllRecentlyPlayed = { invoked = true })

        scrollToSection(3)
        composeRule.onNodeWithText("Recently Played").performClick()

        assert(invoked) { "onSeeAllRecentlyPlayed was not invoked" }
    }
}
