package com.elsfm.mobile.core.media

import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaItemFactoryTest {

    @Test
    fun `builds a playable media item from a track`() {
        val track = Track(
            id = 1192,
            name = "Phul Phulyo Bana Pakhama Ashu Jhajhalkyo",
            // Already absolute - Track.image is decoded via ImageUrlSerializer, which
            // normalizes the backend's relative paths to full URLs at deserialize time,
            // so toMediaItem() must not prefix baseUrl onto it again.
            image = "https://www.elsfm.com/storage/track_image_media/abc.jpeg",
            durationMs = 174000,
            src = "storage/track_media/9cw1dTF9NOayHdT4HdyCydn1jgaVJh7Cm9xu4waT.mp3",
            artists = listOf(Artist(id = 30, name = "Sunday School Songs")),
        )

        val mediaItem = track.toMediaItem()

        assertEquals("1192", mediaItem.mediaId)
        assertEquals(
            "https://www.elsfm.com/storage/track_media/9cw1dTF9NOayHdT4HdyCydn1jgaVJh7Cm9xu4waT.mp3",
            mediaItem.localConfiguration?.uri.toString(),
        )
        assertEquals("Phul Phulyo Bana Pakhama Ashu Jhajhalkyo", mediaItem.mediaMetadata.title.toString())
        assertEquals("Sunday School Songs", mediaItem.mediaMetadata.artist.toString())
        assertEquals(
            "https://www.elsfm.com/storage/track_image_media/abc.jpeg",
            mediaItem.mediaMetadata.artworkUri.toString(),
        )
    }

    @Test
    fun `falls back to the authenticated download endpoint when src is missing`() {
        // The Album/Channel API responses that populate most queues never include `src` at
        // all (confirmed live) - baseUrl + null previously built ".../null", which the
        // backend's SPA resolved to its HTML homepage instead of audio.
        val track = Track(
            id = 1771,
            name = "Harayeko bhera -Nepali Christian Songs - ELSFM",
            image = null,
            durationMs = 289000,
            src = null,
            artists = listOf(Artist(id = 1, name = "ELShaddai Kalimpong")),
        )

        val mediaItem = track.toMediaItem()

        assertEquals(
            "https://www.elsfm.com/api/v1/tracks/1771/download",
            mediaItem.localConfiguration?.uri.toString(),
        )
    }
}
