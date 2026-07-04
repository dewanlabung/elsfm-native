package com.elsfm.mobile.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserializes a real track JSON shape`() {
        val body = """
            {
              "id": 1192,
              "name": "Phul Phulyo Bana Pakhama Ashu Jhajhalkyo",
              "image": "storage/track_image_media/KvU3sDMqRQDPyT1oJ3LhXb6Dh4V0mmgAD5tVtthU.jpeg",
              "duration": 174000,
              "src": "storage/track_media/9cw1dTF9NOayHdT4HdyCydn1jgaVJh7Cm9xu4waT.mp3",
              "artists": [
                {"id": 30, "name": "Sunday School Songs"}
              ]
            }
        """.trimIndent()

        val track = json.decodeFromString<Track>(body)

        assertEquals(1192, track.id)
        assertEquals("Phul Phulyo Bana Pakhama Ashu Jhajhalkyo", track.name)
        assertEquals(174000L, track.durationMs)
        assertEquals("storage/track_media/9cw1dTF9NOayHdT4HdyCydn1jgaVJh7Cm9xu4waT.mp3", track.src)
        assertEquals(1, track.artists.size)
        assertEquals("Sunday School Songs", track.artists[0].name)
    }
}
