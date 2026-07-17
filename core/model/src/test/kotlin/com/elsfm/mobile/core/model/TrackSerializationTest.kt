package com.elsfm.mobile.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserializes a real track JSON shape`() {
        // Shape confirmed via: curl -H "Accept: application/json"
        // "https://www.elsfm.com/api/v1/playlists/1/tracks" — note there is no
        // "src" field on the real payload and "plays" is a string, not an int.
        val body = """
            {
              "id": 1192,
              "name": "Phul Phulyo Bana Pakhama Ashu Jhajhalkyo",
              "image": "storage/track_image_media/KvU3sDMqRQDPyT1oJ3LhXb6Dh4V0mmgAD5tVtthU.jpeg",
              "number": 6,
              "duration": 174000,
              "plays": "1154",
              "popularity": 0,
              "owner_id": "1",
              "model_type": "track",
              "artists": [
                {"id": 30, "name": "Sunday School Songs", "image_small": "storage/artist/x.jpg", "verified": true, "disabled": false, "model_type": "artist"}
              ]
            }
        """.trimIndent()

        val track = json.decodeFromString<Track>(body)

        assertEquals(1192, track.id)
        assertEquals("Phul Phulyo Bana Pakhama Ashu Jhajhalkyo", track.name)
        assertEquals(174000L, track.durationMs)
        assertEquals(null, track.src)
        assertEquals("1154", track.plays)
        assertEquals(1, track.artists.size)
        assertEquals("Sunday School Songs", track.artists[0].name)
    }

    @Test
    fun `treats a null duration as zero instead of failing to parse`() {
        // Regression guard: confirmed live on page 3 of the "2015 EL Shaddai Youth Camp
        // Songs" playlist - a real track had "duration": null. A non-nullable Long
        // previously failed to decode this, which crashed parsing for the whole page
        // (not just that one track), silently stalling playlist auto-load partway through.
        val body = """
            {
              "id": 403,
              "name": "Dishahen bani hriday baralenchha",
              "image": null,
              "duration": null,
              "plays": "37",
              "artists": []
            }
        """.trimIndent()

        val track = json.decodeFromString<Track>(body)

        assertEquals(403, track.id)
        assertEquals(0L, track.durationMs)
    }
}
