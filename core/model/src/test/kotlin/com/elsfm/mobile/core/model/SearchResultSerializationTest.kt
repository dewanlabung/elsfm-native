package com.elsfm.mobile.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchResultSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `SearchResult deserializes TrackResult`() {
        val json = """
            {
              "track": {
                "id": 100,
                "name": "Test Track",
                "duration": 180000,
                "src": "test.mp3",
                "image": "test.jpg",
                "artists": [{"id": 1, "name": "Artist"}]
              }
            }
        """.trimIndent()

        val result = this.json.decodeFromString<SearchResult>(json)

        assertTrue(result is SearchResult.TrackResult)
    }
}
