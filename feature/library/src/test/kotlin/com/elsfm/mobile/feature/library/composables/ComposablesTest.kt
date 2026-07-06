package com.elsfm.mobile.feature.library.composables

import androidx.compose.ui.test.junit4.createComposeRule
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.Track
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ComposablesTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playlistCardRenders() {
        val playlist = Playlist(id = 1, name = "Test Playlist", image = null)
        composeRule.setContent {
            PlaylistCard(playlist = playlist, onClick = {})
        }
    }

    @Test
    fun albumCardRenders() {
        val album = Album(id = 1, name = "Test Album", image = null, releaseDate = "2024-01-01")
        composeRule.setContent {
            AlbumCard(album = album, onClick = {})
        }
    }

    @Test
    fun artistCardRenders() {
        val artist = Artist(id = 1, name = "Test Artist", image = null)
        composeRule.setContent {
            ArtistCard(artist = artist, onClick = {})
        }
    }

    @Test
    fun trackListItemRenders() {
        val track = Track(
            id = 1,
            name = "Test Track",
            image = null,
            durationMs = 180000,
            artists = emptyList(),
            src = null,
            plays = null,
        )
        composeRule.setContent {
            TrackListItem(track = track, onClick = {})
        }
    }

    @Test
    fun sectionHeaderRenders() {
        composeRule.setContent {
            SectionHeader(title = "Featured", onSeeAllClick = {})
        }
    }

    @Test
    fun blurredBackgroundRenders() {
        composeRule.setContent {
            BlurredBackground(imageUrl = null) {}
        }
    }
}
